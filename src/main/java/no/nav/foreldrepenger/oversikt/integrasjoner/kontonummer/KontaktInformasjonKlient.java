package no.nav.foreldrepenger.oversikt.integrasjoner.kontonummer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.felles.integrasjon.rest.NavHeaders;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;

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

    public KontonummerDto hentRegistertKontonummer() {
        try {
            var request = RestRequest.newGET(endpoint, restConfig)
                    .otherCallId(NavHeaders.HEADER_NAV_LOWER_CALL_ID)
                    .timeout(Duration.ofSeconds(3));
            return restClient.send(request, KontonummerDto.class);
        } catch (ManglerTilgangException m) {
            LOG.warn("Mangler tilgang til kontoregister", m);
            return KontonummerDto.UKJENT;
        } catch (Exception e) {
            LOG.info("Oppslag av kontonummer feilet! Forsetter uten kontonummer!", e);
            return KontonummerDto.UKJENT;
        }
    }

}