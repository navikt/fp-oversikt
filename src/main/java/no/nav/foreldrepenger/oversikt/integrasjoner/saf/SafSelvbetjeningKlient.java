package no.nav.foreldrepenger.oversikt.integrasjoner.saf;


import jakarta.enterprise.context.ApplicationScoped;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.felles.integrasjon.safselvbetjening.AbstractSafSelvbetjeningKlient;

@ApplicationScoped
@RestClientConfig(
        tokenConfig = TokenFlow.ADAPTIVE,
        endpointProperty = "safselvbetjening.base.url",
        endpointDefault = "https://safselvbetjening.prod-fss-pub.nais.io",
        scopesProperty = "safselvbetjening.scopes",
        scopesDefault = "api://prod-fss.teamdokumenthandtering.safselvbetjening/.default"
)
public class SafSelvbetjeningKlient extends AbstractSafSelvbetjeningKlient {

    public SafSelvbetjeningKlient() {
        super();
    }

}
