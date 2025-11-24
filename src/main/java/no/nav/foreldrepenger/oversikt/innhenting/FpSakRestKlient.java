package no.nav.foreldrepenger.oversikt.innhenting;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;
import no.nav.foreldrepenger.oversikt.arkiv.DokumentTypeHistoriske;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.innhenting.beregning.FpSakBeregningDto;
import no.nav.foreldrepenger.oversikt.innhenting.inntektsmelding.FpSakInntektsmeldingDto;
import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, application = FpApplication.FPSAK)
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
    public List<DokumentTypeHistoriske> hentManglendeVedlegg(Saksnummer saksnummer) {
        var uri = uri("/fpoversikt/manglendeVedlegg", saksnummer);
        var request = RestRequest.newGET(uri, restConfig);
        return restClient.sendReturnList(request, DokumentTypeHistoriske.class);
    }

    @Override
    public List<FpSakInntektsmeldingDto> hentInntektsmeldinger(Saksnummer saksnummer) {
        var uri = uri("/fpoversikt/inntektsmeldinger", saksnummer);
        var request = RestRequest.newGET(uri, restConfig);
        return restClient.sendReturnList(request, FpSakInntektsmeldingDto.class);
    }

    @Override
    public Optional<FpSakBeregningDto> hentBeregning(Saksnummer saksnummer) {
        var uri = uri("/fpoversikt/beregning", saksnummer);
        var request = RestRequest.newGET(uri, restConfig);

        // TODO: hvordan?
        var res  = restClient.send(request, FpSakBeregningDto.class);
        if (res == null) {
            return Optional.empty();
        }
        return Optional.of(res);
    }

    private URI uri(String path, Saksnummer saksnummer) {
        return UriBuilder.fromUri(restConfig.endpoint()).path(FPSAK_API).path(path).queryParam("saksnummer", saksnummer.value()).build();
    }
}
