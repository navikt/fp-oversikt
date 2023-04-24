package no.nav.foreldrepenger.oversikt.innhenting;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.UriBuilder;

import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(
    tokenConfig = TokenFlow.AZUREAD_CC,
    endpointProperty = "fpsak.base.url",
    endpointDefault = "https://fpsak-api.prod-fss-pub.nais.io/fpsak",
    scopesProperty = "fpsak.scopes",
    scopesDefault = "api://prod-fss.teamforeldrepenger.fpsak/.default")
class FpSakKlient {

    private static final String FPSAK_API = "/api";

    private final RestClient restClient;
    private final RestConfig restConfig;

    FpSakKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(this.getClass());
    }

    BehandlingDto hentBehandling(UUID id) {
        var uri = UriBuilder.fromUri(restConfig.fpContextPath())
            .path(FPSAK_API)
            .path("/formidling/ressurser")
            .queryParam("behandlingId", id.toString())
            .build();
        var request = RestRequest.newGET(uri, restConfig);
        return restClient.sendReturnOptional(request, BehandlingDto.class)
            .orElseThrow(() -> new IllegalStateException("Klarte ikke hente behandling: " + id));
    }

    private record BehandlingDto(UUID uuid, LocalDateTime opprettet) {
    }
}
