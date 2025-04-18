package no.nav.foreldrepenger.oversikt.integrasjoner.ereg;

import jakarta.enterprise.context.Dependent;
import no.nav.vedtak.felles.integrasjon.organisasjon.AbstractOrganisasjonKlient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

/*
 * Dokumentasjon https://confluence.adeo.no/display/FEL/EREG+-+Tjeneste+REST+ereg.api
 * Swagger https://modapp-q1.adeo.no/ereg/api/swagger-ui.html#/
 */

@Dependent
@RestClientConfig(tokenConfig = TokenFlow.NO_AUTH_NEEDED, endpointProperty = "organisasjon.rs.url",
    endpointDefault = "https://ereg-services.prod-fss-pub.nais.io/api/v2/organisasjon")
public class OrganisasjonRestKlient extends AbstractOrganisasjonKlient {

    public OrganisasjonRestKlient() {
        super();
    }
}
