package no.nav.foreldrepenger.oversikt.innhenting;

import java.net.URI;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;
import no.nav.foreldrepenger.common.domain.felles.DokumentType;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.innhenting.inntektsmelding.Inntektsmelding;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

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
        var uri = uri("/fpoversikt/sak", saksnummer);
        var request = RestRequest.newGET(uri, restConfig);
        return restClient.sendReturnOptional(request, Sak.class)
            .orElseThrow(() -> new IllegalStateException("Klarte ikke hente sak: " + saksnummer));
    }

    @Override
    public List<DokumentType> hentManglendeVedlegg(Saksnummer saksnummer) {
        var uri = uri("/fpoversikt/manglendeVedlegg", saksnummer);
        var request = RestRequest.newGET(uri, restConfig);
        return restClient.sendReturnList(request, DokumentType.class);
    }

    @Override
    public List<Inntektsmelding> hentInntektsmeldinger(Saksnummer saksnummer) {
        var uri = uri("/fpoversikt/inntektsmeldinger", saksnummer);
        var request = RestRequest.newGET(uri, restConfig);
        return restClient.sendReturnList(request, Inntektsmelding.class);
    }

    private URI uri(String path, Saksnummer saksnummer) {
        return UriBuilder.fromUri(restConfig.endpoint()).path(FPSAK_API).path(path).queryParam("saksnummer", saksnummer.value()).build();
    }
}
