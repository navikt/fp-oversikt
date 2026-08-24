package no.nav.foreldrepenger.oversikt.integrasjoner.brreg;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Comparator;
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

    // Dolly har ingen mock for Brregs REST-API, derfor er integrasjonen deaktivert i dev.
    private static final boolean BRREG_DEAKTIVERT = ENV.isProd() || ENV.isDev();

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

    public BrregRollerTjeneste() {
        this(RestClient.client(), RestConfig.forClient(BrregRollerTjeneste.class));
    }

    public BrregRollerTjeneste(RestClient sender, RestConfig config) {
        this.restConfig = config;
        this.sender = sender;
        this.maskinportenResource = restConfig.endpoint().toString() + AUTORISERT_API; // annen ressurs for bruk i preprod
        this.rolleutskriftEndpoint = UriBuilder.fromUri(restConfig.endpoint()).path(ROLLEUTSKRIFT_URL).build();
    }

    public List<BrregSelvstendigNæring> finnSelvstendigNæring(Fødselsnummer fødselsnummer) {
        return hentRollerForPerson(fødselsnummer).stream()
            .map(this::finnSelvstendigNæring)
            .sorted(Comparator.comparing(BrregSelvstendigNæring::navn, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(BrregSelvstendigNæring::organisasjonsnummer, Comparator.nullsLast(String::compareTo)))
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
        if (BRREG_DEAKTIVERT) {
            return Optional.empty();
        }
        var cachetEnhet = orgnummer == null ? null : CACHE_ENHET.get(orgnummer);
        if (cachetEnhet != null) {
            return Optional.of(cachetEnhet);
        }
        try {
            var request = RestRequest.newGET(target, restConfig);
            var respons = sender.sendReturnOptional(request, BrregEnhetDto.class);
            respons.ifPresent(r -> CACHE_ENHET.put(r.organisasjonsnummer(), r));
            return respons;
        } catch (Exception _) {
            var maskertPath = target.getPath().replace(orgnummer, maskerOrgnr(orgnummer));
            LOG.warn("Uvanlig feil ved kall mot Brreg enhetsoppslag {}", maskertPath);
            return Optional.empty();
        }
    }

    public List<BrregRolleutskriftDto.EnhetDto> hentRollerForPerson(Fødselsnummer fødselsnummer) {
        if (BRREG_DEAKTIVERT) {
            return List.of();
        }
        var cachetRolleutskrift = CACHE_ROLLEUTSKRIFT.get(fødselsnummer.value());
        if (cachetRolleutskrift != null) {
            return cachetRolleutskrift;
        }
        var respons = gjørPersonKallTilBrreg(fødselsnummer);
        var resultat = respons.map(BrregRolleutskriftDto::enheter).orElse(List.of()).stream()
            .filter(BrregRollerMapper::erSelvstendigNæringsdrivende)
            .toList();
        LOG.info("FPOVERSIKT vellykket kall mot brreg direkte rolleutskrift. Fikk {}", resultat.size());
        CACHE_ROLLEUTSKRIFT.put(fødselsnummer.value(), resultat);
        return resultat;
    }

    private Optional<BrregRolleutskriftDto> gjørPersonKallTilBrreg(Fødselsnummer fødselsnummer) {
        try {
            var method = new RestRequest.Method(RestRequest.WebMethod.POST, HttpRequest.BodyPublishers.ofString(fødselsnummer.value()));
            var request = RestRequest.newRequest(method, rolleutskriftEndpoint, restConfig)
                .setAndReplaceHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN)
                .otherAuthorizationSupplier(() -> MaskinportenTokenKlient.instance()
                    .hentMaskinportenToken(ROLLEUTSKRIFT_SCOPE, maskinportenResource)
                    .token());
            return sender.sendReturnOptional(request, BrregRolleutskriftDto.class);
        } catch (Exception e) {
            if (e instanceof IntegrasjonException ie && Response.Status.NOT_FOUND.getStatusCode() == ie.getStatusCode()) {
                return Optional.empty();
            }
            LOG.warn("Kall mot brreg direkte rolleutskrift feilet for innlogget bruker. Feiltype: {}",
                e.getClass().getSimpleName());
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Kall mot Brreg rolleutskrift feilet.", e);
        }
    }

    static String maskerOrgnr(String orgnummer) {
        if (orgnummer == null) {
            return "";
        }
        var length = orgnummer.length();
        if (length <= 4) {
            return "*".repeat(length);
        }
        return "*".repeat(length - 3) + orgnummer.substring(length - 3);
    }

}
