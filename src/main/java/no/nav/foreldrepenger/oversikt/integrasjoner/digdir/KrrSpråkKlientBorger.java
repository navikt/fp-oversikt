package no.nav.foreldrepenger.oversikt.integrasjoner.digdir;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

/*
 * https://github.com/navikt/digdir-krr
 * https://digdir-krr-proxy.intern.dev.nav.no/swagger-ui/index.html#/personer-controller/postPersoner
 */

@ApplicationScoped
@RestClientConfig(
    tokenConfig = TokenFlow.ADAPTIVE,
    endpointProperty = "krr.rs.uri",
    endpointDefault = "http://digdir-krr-proxy.team-rocket/rest/v1/personer",
    scopesProperty = "krr.rs.scopes",
    scopesDefault = "api://prod-gcp.team-rocket.digdir-krr-proxy/.default"
)
public class KrrSpråkKlientBorger extends KrrSpråkKlient {

    public KrrSpråkKlientBorger() {
        this(RestClient.client());
    }

    public KrrSpråkKlientBorger(RestClient restClient) {
        super(restClient, RestConfig.forClient(KrrSpråkKlientSystem.class));
    }
}
