package no.nav.foreldrepenger.oversikt.integrasjoner.kontonummer;

import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.felles.integrasjon.rest.NavHeaders;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

/*
 * https://github.com/navikt/sokos-kontoregister-person
 */

@ApplicationScoped
@RestClientConfig(
        tokenConfig = TokenFlow.ADAPTIVE,
        endpointProperty = "sokos.kontoregister.person.url",
        endpointDefault = "http://sokos-kontoregister-person.okonomi/api/borger/v1/hent-aktiv-konto",
        scopesProperty = "sokos.kontoregister.person.rs.scopes",
        scopesDefault = "api://prod-gcp.okonomi.sokos-kontoregister-person/.default"
)
public class KontaktInformasjonKlient {
    private static final Logger LOG = LoggerFactory.getLogger(KontaktInformasjonKlient.class);
    private final URI endpoint;
    private final RestClient restClient;
    private final RestConfig restConfig;

    public KontaktInformasjonKlient() {
        this(RestClient.client());
    }

    public KontaktInformasjonKlient(RestClient restClient) {
        this.restClient = restClient;
        this.restConfig = RestConfig.forClient(KontaktInformasjonKlient.class);
        this.endpoint = UriBuilder.fromUri(restConfig.endpoint()).build();
    }

    public KontonummerDto hentRegistertKontonummerMedFallback() {
        try {
            var request = RestRequest.newGET(endpoint, restConfig)
                    .otherCallId(NavHeaders.HEADER_NAV_LOWER_CALL_ID)
                    .timeout(Duration.ofSeconds(3));
            var response = restClient.sendReturnUnhandled(request);
            var statusKode = response.statusCode();
            if (statusKode == Response.Status.NOT_FOUND.getStatusCode()) {
                // Person har ikke kontonummer registert i kontoregisteret
                return KontonummerDto.UKJENT;
            }
            if (statusKode == HttpURLConnection.HTTP_FORBIDDEN) {
                LOG.warn("Mangler tilgang til kontoregister. Forsetter med uten kontonummer!");
                return KontonummerDto.UKJENT;
            }
            if (statusKode > 300) {
                throw new IntegrasjonException("FP-468817", "Feil ved henting av kontonummer for person. " +  response.body());
            }
            if (response.body() == null || response.body().isEmpty()) {
                LOG.info("Tomt svar fra kontoregister. Forsetter uten kontonummer!");
                return KontonummerDto.UKJENT;
            }
            return DefaultJsonMapper.fromJson(response.body(), KontonummerDto.class);
        } catch (Exception e) {
            LOG.warn("Oppslag av kontonummer feilet! Forsetter uten kontonummer!", e);
            return KontonummerDto.UKJENT;
        }
    }
}
