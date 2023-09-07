package no.nav.foreldrepenger.oversikt.innhenting.tilbakekreving;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

import java.util.Optional;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, endpointProperty = "fptilbake.base.url", endpointDefault = "https://fptilbake-api.prod-fss-pub.nais.io/fptilbake", scopesProperty = "fptilbake.scopes", scopesDefault = "api://prod-fss.teamforeldrepenger.fptilbake/.default")
public class FptilbakeRestKlient implements FptilbakeTjeneste {

    private static final String FPTILBAKE_API = "/api";

    private final RestClient restClient;
    private final RestConfig restConfig;

    FptilbakeRestKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(this.getClass());
    }

    @Override
    public Optional<Tilbakekreving> hent(Saksnummer saksnummer) {
        var uri = UriBuilder.fromUri(restConfig.endpoint()).path(FPTILBAKE_API).path("/fpoversikt/sak")
            .queryParam("saksnummer", saksnummer.value()).build();
        var request = RestRequest.newGET(uri, restConfig);
        return restClient.sendReturnOptional(request, Tilbakekreving.class);
    }
}
