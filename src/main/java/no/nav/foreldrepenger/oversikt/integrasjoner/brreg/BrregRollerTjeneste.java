package no.nav.foreldrepenger.oversikt.integrasjoner.brreg;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.sikkerhet.oidc.token.impl.MaskinportenTokenKlient;
import no.nav.vedtak.util.LRUCache;

@Dependent // Vennligst la denne henge under en ApplicationScoped så cache blir værende
@RestClientConfig(tokenConfig = TokenFlow.NO_AUTH_NEEDED, endpointProperty = "brreg.direct.url", endpointDefault = "https://data.brreg.no/enhetsregisteret")
public class BrregRollerTjeneste {

    private static final Environment ENV = Environment.current();
    private static final Logger LOG = LoggerFactory.getLogger(BrregRollerTjeneste.class);

    private static final String AUTORISERT_API = "/autorisert-api";

    private static final String ROLLEUTSKRIFT_SCOPE = "brreg:data:enhetsregisteret:roller:person:oppslag:fnr";
    private static final String ROLLEUTSKRIFT_URL = AUTORISERT_API + "/personer/rolleutskrift";

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);
    private static final LRUCache<String, BrregEnhetDto> CACHE_ENHET = new LRUCache<>(200, CACHE_ELEMENT_LIVE_TIME_MS);
    private static final LRUCache<String, List<BrregRolleutskriftDto.EnhetDto>> CACHE_ROLLEUTSKRIFT = new LRUCache<>(100, CACHE_ELEMENT_LIVE_TIME_MS);

    private final RestClient sender;
    private final RestConfig restConfig;
    private final String maskinportenResource;
    private final URI rolleutskriftEndpoint;
    private MaskinportenTokenKlient tokenKlient;

    public BrregRollerTjeneste() {
        this(RestClient.client(), RestConfig.forClient(BrregRollerTjeneste.class));
    }

    public BrregRollerTjeneste(RestClient sender, RestConfig config) {
        this.restConfig = config;
        this.sender = sender;
        this.maskinportenResource = restConfig.endpoint().toString() + AUTORISERT_API; // annen ressurs for bruk i preprod
        this.rolleutskriftEndpoint = UriBuilder.fromUri(restConfig.endpoint()).path(ROLLEUTSKRIFT_URL).build();
        this.tokenKlient = null;
    }

    public List<BrregSelvstendigNæring> finnSelvstendigNæring(Fødselsnummer fødselsnummer) {
        return hentRollerForPerson(fødselsnummer).stream()
            .map(this::finnSelvstendigNæring)
            .toList();
    }

    private BrregSelvstendigNæring finnSelvstendigNæring(BrregRolleutskriftDto.EnhetDto enhet) {
        var enhetsdata = Optional.ofNullable(enhet._links())
            .map(BrregRolleutskriftDto.LinksDto::enhet)
            .map(BrregRolleutskriftDto.LinkDto::href)
            .map(URI::create)
            .flatMap(uri -> finnEnhetsinfoFraLink(enhet.organisasjonsnummer(), uri))
            .orElse(null);
        return BrregRollerMapper.mapSelvstendigNæring(enhet, enhetsdata);
    }

    public Optional<BrregEnhetDto> finnEnhetsinfoFraLink(String orgnummer, URI target) {
        if (!ENV.isProd()) {
            return Optional.empty();
        }
        if (orgnummer != null && CACHE_ENHET.get(orgnummer) != null) {
            return Optional.ofNullable(CACHE_ENHET.get(orgnummer));
        }
        try {
            var request = RestRequest.newGET(target, restConfig);
            var respons = sender.sendReturnOptional(request, BrregEnhetDto.class);
            respons.ifPresent(r -> CACHE_ENHET.put(r.organisasjonsnummer(), r));
            return respons;
        } catch (Exception e) {
            LOG.warn("FPRISK Uvanlig feil ved kall mot brreg direkte enhetslink for {}. Fikk feilmelding: ", target, e);
            return Optional.empty();
        }
    }

    public List<BrregRolleutskriftDto.EnhetDto> hentRollerForPerson(Fødselsnummer fødselsnummer) {
        if (!ENV.isProd() || fødselsnummer.value() == null) {
            return List.of();
        }
        if (CACHE_ROLLEUTSKRIFT.get(fødselsnummer.value()) != null) {
            return CACHE_ROLLEUTSKRIFT.get(fødselsnummer.value());
        }
        if (tokenKlient == null) {
            tokenKlient = MaskinportenTokenKlient.instance();
        }
        var respons = gjørPersonKallTilBrreg(fødselsnummer);
        var resultat = respons.map(BrregRolleutskriftDto::enheter).orElse(List.of()).stream()
            .filter(BrregRollerMapper::erSelvstendigNæringsdrivende)
            .toList();
        LOG.info("FPRISK vellykket kall mot brreg direkte rolleutskrift. Fikk {}", resultat.size());
        CACHE_ROLLEUTSKRIFT.put(fødselsnummer.value(), resultat);
        return resultat;
    }

    private Optional<BrregRolleutskriftDto> gjørPersonKallTilBrreg(Fødselsnummer fødselsnummer) {
        try {
            var method = new RestRequest.Method(RestRequest.WebMethod.POST, HttpRequest.BodyPublishers.ofString(fødselsnummer.value()));
            var request = RestRequest.newRequest(method, rolleutskriftEndpoint, restConfig)
                .setAndReplaceHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN)
                .otherAuthorizationSupplier(() -> tokenKlient.hentMaskinportenToken(ROLLEUTSKRIFT_SCOPE, maskinportenResource).token());
            return sender.sendReturnOptional(request, BrregRolleutskriftDto.class);
        } catch (Exception e) {
            if (e instanceof IntegrasjonException ie && Response.Status.NOT_FOUND.getStatusCode() == ie.getStatusCode()) {
                return Optional.empty();
            } else {
                var ie = e.getMessage();
                var vasketFeil = ie.replace(fødselsnummer.value(), fødselsnummer.toString());
                LOG.info("Kall mot brreg direkte rolleutskrift feilet. Fikk feilmelding: {}", vasketFeil);
                return Optional.empty();
            }
        }
    }


}
