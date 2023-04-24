package no.nav.foreldrepenger.oversikt.saker;

import javax.enterprise.context.Dependent;

import no.nav.vedtak.felles.integrasjon.person.AbstractPersonKlient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@RestClientConfig(
    tokenConfig = TokenFlow.ADAPTIVE,
    endpointProperty = "pdl.base.url",
    endpointDefault = "https://pdl-api.prowdawdd-fss-pub.nais.io/graphql",
    scopesProperty = "pdl.scopes",
    scopesDefault = "api://prod-fss.pdl.pdl-api/.default")
@Dependent
public class PdlKlient extends AbstractPersonKlient {

}
