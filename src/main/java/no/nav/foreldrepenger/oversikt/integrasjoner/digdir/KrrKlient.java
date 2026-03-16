package no.nav.foreldrepenger.oversikt.integrasjoner.digdir;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;
import no.nav.vedtak.felles.integrasjon.rest.NavHeaders;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.util.LRUCache;

/*
 * https://github.com/navikt/digdir-krr
 * https://digdir-krr-proxy.intern.dev.nav.no/swagger-ui/index.html#/personer-controller/postPersoner
 */
@ApplicationScoped
@RestClientConfig(
    tokenConfig = TokenFlow.AZUREAD_CC,
    endpointProperty = "krr.rs.uri",
    endpointDefault = "http://digdir-krr-proxy.team-rocket/rest/v1/personer",
    scopesProperty = "krr.rs.scopes",
    scopesDefault = "api://prod-gcp.team-rocket.digdir-krr-proxy/.default"
)
public class KrrKlient {
    private static final Logger LOG = LoggerFactory.getLogger(KrrKlient.class);
    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(60, TimeUnit.MINUTES);
    private static final LRUCache<String, Kontaktinformasjoner.Kontaktinformasjon> KONTAKTINFORMASJON_CACHE = new LRUCache<>(2000, CACHE_ELEMENT_LIVE_TIME_MS);

    private final URI endpoint;
    private final RestClient restClient;
    private final RestConfig restConfig;

    public KrrKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(KrrKlient.class);
        this.endpoint = UriBuilder.fromUri(restConfig.endpoint())
            .queryParam("inkluderSikkerDigitalPost", "false")
            .build();
    }

    public Optional<Kontaktinformasjoner.Kontaktinformasjon> hentKontaktinformasjon(String fnr) {
        return  Optional.ofNullable(KONTAKTINFORMASJON_CACHE.get(fnr))
                .or(() -> {
                    var kontaktinformasjonOpt = hentKontaktinformasjonFraKRR(fnr);
                    kontaktinformasjonOpt.ifPresent(k -> KONTAKTINFORMASJON_CACHE.put(fnr, k));
                    return kontaktinformasjonOpt;
                });
    }

    private Optional<Kontaktinformasjoner.Kontaktinformasjon> hentKontaktinformasjonFraKRR(String fnr) {
        var request = RestRequest.newPOSTJson(new Personidenter(List.of(fnr)), endpoint, restConfig)
                .otherCallId(NavHeaders.HEADER_NAV_CALL_ID)
                .timeout(Duration.ofSeconds(3)); // Kall langt avgårde - blokkerer ofte til 3*timeout. Request inn til fpsak har timeout 20s.
        var respons = restClient.send(request, Kontaktinformasjoner.class);
        if (respons.feil() != null && !respons.feil().isEmpty()) {
            var feilkode = respons.feil().get(fnr);
            if (Kontaktinformasjoner.FeilKode.person_ikke_funnet.equals(feilkode)) {
                LOG.info("KrrKlient: fant ikke bruker, returnerer default");
            } else {
                LOG.warn("KrrKlient: Uventet feil ved kall til KRR {}, returnerer default", feilkode);
            }
            return Optional.empty();
        }
        return Optional.of(respons.personer().get(fnr));
    }

    record Personidenter(List<String> personidenter) {
    }
}
