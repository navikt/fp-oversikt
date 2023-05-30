package no.nav.foreldrepenger.oversikt.saker;

import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.pdl.Adressebeskyttelse;
import no.nav.pdl.AdressebeskyttelseResponseProjection;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.PersonResponseProjection;
import no.nav.vedtak.felles.integrasjon.person.AbstractPersonKlient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@RestClientConfig(
    tokenConfig = TokenFlow.AZUREAD_CC,
    endpointProperty = "pdl.base.url",
    endpointDefault = "https://pdl-api.prod-fss-pub.nais.io/graphql",
    scopesProperty = "pdl.scopes",
    scopesDefault = "api://prod-fss.pdl.pdl-api/.default")
@Dependent
public class PdlKlientSystem extends AbstractPersonKlient implements AdresseBeskyttelseOppslag {

    @Override
    public no.nav.foreldrepenger.oversikt.tilgangskontroll.AdresseBeskyttelse adresseBeskyttelse(Fødselsnummer fnr) {
        var request = new HentPersonQueryRequest();
        request.setIdent(fnr.value());
        var projection = new PersonResponseProjection()
            .adressebeskyttelse(new AdressebeskyttelseResponseProjection().gradering());
        var person = hentPerson(request, projection);

        var gradering = person.getAdressebeskyttelse().stream()
            .map(Adressebeskyttelse::getGradering)
            .collect(Collectors.toSet());

        return new no.nav.foreldrepenger.oversikt.tilgangskontroll.AdresseBeskyttelse(gradering);
    }

}
