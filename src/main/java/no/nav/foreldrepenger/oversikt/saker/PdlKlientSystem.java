package no.nav.foreldrepenger.oversikt.saker;

import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.AdresseBeskyttelse;
import no.nav.pdl.Adressebeskyttelse;
import no.nav.pdl.AdressebeskyttelseGradering;
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
    public Optional<AdresseBeskyttelse> adresseBeskyttelse(Fødselsnummer fnr) {
        var request = new HentPersonQueryRequest();
        request.setIdent(fnr.value());
        var projection = new PersonResponseProjection()
            .adressebeskyttelse(new AdressebeskyttelseResponseProjection().gradering());
        var person = hentPerson(request, projection);

        if (person == null) {
            return Optional.empty();
        }

        var gradering = person.getAdressebeskyttelse().stream()
                .map(Adressebeskyttelse::getGradering)
                .map(PdlKlientSystem::tilGradering)
                .collect(Collectors.toSet());
        return Optional.of(new AdresseBeskyttelse(gradering));
    }

    private static AdresseBeskyttelse.Gradering tilGradering(AdressebeskyttelseGradering adressebeskyttelseGradering) {
        if (adressebeskyttelseGradering == null) {
            return AdresseBeskyttelse.Gradering.UGRADERT;
        }
        return switch (adressebeskyttelseGradering) {
            case STRENGT_FORTROLIG_UTLAND, STRENGT_FORTROLIG, FORTROLIG -> AdresseBeskyttelse.Gradering.GRADERT;
            case UGRADERT -> AdresseBeskyttelse.Gradering.UGRADERT;
        };
    }

}
