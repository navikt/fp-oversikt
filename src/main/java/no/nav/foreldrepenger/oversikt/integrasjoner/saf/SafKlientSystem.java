package no.nav.foreldrepenger.oversikt.integrasjoner.saf;


import jakarta.enterprise.context.Dependent;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.felles.integrasjon.saf.AbstractSafKlient;

@RestClientConfig(
    tokenConfig = TokenFlow.AZUREAD_CC,
    endpointProperty = "saf.base.url",
    endpointDefault = "https://saf.prod-fss-pub.nais.io",
    scopesProperty = "saf.scopes",
    scopesDefault = "api://prod-fss.teamdokumenthandtering.saf/.default")
@Dependent
public class SafKlientSystem extends AbstractSafKlient {

    public SafKlientSystem() {
        super();
    }

}
