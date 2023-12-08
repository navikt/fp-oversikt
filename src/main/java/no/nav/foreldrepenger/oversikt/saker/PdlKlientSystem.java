package no.nav.foreldrepenger.oversikt.saker;

import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
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
class PdlKlientSystem extends AbstractPersonKlient implements PersonOppslagSystem {

    @Override
    public Fødselsnummer fødselsnummer(AktørId aktørId) {
        return new Fødselsnummer(hentPersonIdentForAktørId(aktørId.value()).orElseThrow());
    }

    @Override
    public AktørId aktørId(Fødselsnummer fnr) {
        return new AktørId(hentAktørIdForPersonIdent(fnr.value()).orElseThrow());
    }

    @Override
    public AdresseBeskyttelse adresseBeskyttelse(Fødselsnummer fnr) throws BrukerIkkeFunnetIPdlException {
        var request = new HentPersonQueryRequest();
        request.setIdent(fnr.value());
        var projection = new PersonResponseProjection()
            .adressebeskyttelse(new AdressebeskyttelseResponseProjection().gradering());
        var person = hentPerson(request, projection, true);

        if (person == null) {
            throw new BrukerIkkeFunnetIPdlException();
        }

        var gradering = person.getAdressebeskyttelse().stream()
                .map(Adressebeskyttelse::getGradering)
                .map(PdlKlientSystem::tilGradering)
                .collect(Collectors.toSet());
        return new AdresseBeskyttelse(gradering);
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
