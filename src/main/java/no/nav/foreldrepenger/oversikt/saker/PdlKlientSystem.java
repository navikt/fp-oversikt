package no.nav.foreldrepenger.oversikt.saker;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.AdresseBeskyttelse;
import no.nav.pdl.Adressebeskyttelse;
import no.nav.pdl.AdressebeskyttelseGradering;
import no.nav.pdl.AdressebeskyttelseResponseProjection;
import no.nav.pdl.ForelderBarnRelasjonResponseProjection;
import no.nav.pdl.ForelderBarnRelasjonRolle;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.Navn;
import no.nav.pdl.NavnResponseProjection;
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

    @Override
    public String navn(String ident) {
        var request = new HentPersonQueryRequest();
        request.setIdent(ident);
        var projection = new PersonResponseProjection()
            .navn(new NavnResponseProjection().fornavn().mellomnavn().etternavn());
        var person = hentPerson(request, projection, true);

        if (person == null) {
            return "Ukjent person";
        }
        return person.getNavn().stream()
            .map(PdlKlientSystem::mapNavn)
            .filter(Objects::nonNull)
            .findFirst().orElse("Ukjent navn");
    }

    @Override
    public boolean barnHarDisseForeldrene(Fødselsnummer barn, Fødselsnummer mor, Fødselsnummer annenForelder) {
        var request = new HentPersonQueryRequest();
        request.setIdent(barn.value());
        var projection = new PersonResponseProjection()
            .forelderBarnRelasjon(new ForelderBarnRelasjonResponseProjection().relatertPersonsIdent().relatertPersonsIdent());
        var person = hentPerson(request, projection, true);

        if (person == null) {
            return false;
        }
        var forventetMor = person.getForelderBarnRelasjon().stream()
            .filter(f -> ForelderBarnRelasjonRolle.MOR.equals(f.getRelatertPersonsRolle()))
            .filter(f -> Objects.equals(f.getRelatertPersonsIdent(), mor.value()))
            .findFirst();
        var forventetAnnenForelder = person.getForelderBarnRelasjon().stream()
            .filter(f -> ForelderBarnRelasjonRolle.FAR.equals(f.getRelatertPersonsRolle()) || ForelderBarnRelasjonRolle.MEDMOR.equals(f.getRelatertPersonsRolle()))
            .filter(f -> Objects.equals(f.getRelatertPersonsIdent(), annenForelder.value()))
            .findFirst();
        return forventetMor.isPresent() && forventetAnnenForelder.isPresent();
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

    private static String mapNavn(Navn navn) {
        if (navn.getFornavn() == null) {
            return null;
        }
        return navn.getFornavn() + leftPad(navn.getMellomnavn()) + leftPad(navn.getEtternavn());
    }

    private static String leftPad(String navn) {
        return Optional.ofNullable(navn).map(n -> " " + navn).orElse("");
    }

}
