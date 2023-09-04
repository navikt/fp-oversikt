package no.nav.foreldrepenger.oversikt.innhenting;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;

import no.nav.foreldrepenger.common.domain.felles.DokumentType;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

import java.util.List;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, endpointProperty = "fpsak.base.url", endpointDefault = "https://fpsak-api.prod-fss-pub.nais.io/fpsak", scopesProperty = "fpsak.scopes", scopesDefault = "api://prod-fss.teamforeldrepenger.fpsak/.default")
class FpSakRestKlient implements FpsakTjeneste {

    private static final String FPSAK_API = "/api";

    private final RestClient restClient;
    private final RestConfig restConfig;

    FpSakRestKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(this.getClass());
    }

    @Override
    public Sak hentSak(Saksnummer saksnummer) {
        var uri = UriBuilder.fromUri(restConfig.endpoint()).path(FPSAK_API).path("/fpoversikt/sak").queryParam("saksnummer", saksnummer.value()).build();
        var request = RestRequest.newGET(uri, restConfig);
        return restClient.sendReturnOptional(request, Sak.class)
            .orElseThrow(() -> new IllegalStateException("Klarte ikke hente sak: " + saksnummer));
    }

    @Override
    public List<DokumentType> hentMangelendeVedlegg(Saksnummer saksnummer) {
        var uri = UriBuilder.fromUri(restConfig.endpoint()).path(FPSAK_API).path("/fpoversikt/manglendeVedlegg").queryParam("saksnummer", saksnummer.value()).build();
        var request = RestRequest.newGET(uri, restConfig);
        return restClient.sendReturnList(request, DokumentType.class);
    }
}
