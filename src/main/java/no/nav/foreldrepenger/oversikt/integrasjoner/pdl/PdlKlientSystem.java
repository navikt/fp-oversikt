package no.nav.foreldrepenger.oversikt.integrasjoner.pdl;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.saker.BrukerIkkeFunnetIPdlException;
import no.nav.foreldrepenger.oversikt.saker.PersonOppslagSystem;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.AdresseBeskyttelse;
import no.nav.pdl.Adressebeskyttelse;
import no.nav.pdl.AdressebeskyttelseGradering;
import no.nav.pdl.AdressebeskyttelseResponseProjection;
import no.nav.pdl.ForelderBarnRelasjonResponseProjection;
import no.nav.pdl.ForelderBarnRelasjonRolle;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.NavnResponseProjection;
import no.nav.pdl.PersonResponseProjection;
import no.nav.vedtak.felles.integrasjon.person.AbstractPersonKlient;
import no.nav.vedtak.felles.integrasjon.person.PersonMappers;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.util.LRUCache;

@RestClientConfig(
    tokenConfig = TokenFlow.AZUREAD_CC,
    endpointProperty = "pdl.base.url",
    endpointDefault = "https://pdl-api.prod-fss-pub.nais.io/graphql",
    scopesProperty = "pdl.scopes",
    scopesDefault = "api://prod-fss.pdl.pdl-api/.default")
@Dependent
public class PdlKlientSystem extends AbstractPersonKlient implements PersonOppslagSystem {

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(60, TimeUnit.MINUTES);
    private static final LRUCache<String, Fødselsnummer> AKTØR_FNR = new LRUCache<>(2000, CACHE_ELEMENT_LIVE_TIME_MS);
    private static final LRUCache<String, AktørId> FNR_AKTØR = new LRUCache<>(2000, CACHE_ELEMENT_LIVE_TIME_MS);
    private static final LRUCache<String, AdresseBeskyttelse> FNR_ADRESSE = new LRUCache<>(2000, CACHE_ELEMENT_LIVE_TIME_MS);
    private static final LRUCache<String, String> IDENT_NAVN = new LRUCache<>(2000, CACHE_ELEMENT_LIVE_TIME_MS);

    @Override
    public Fødselsnummer fødselsnummer(AktørId aktørId) {
        return Optional.ofNullable(AKTØR_FNR.get(aktørId.value()))
            .orElseGet(() -> {
                var fnr = new Fødselsnummer(hentPersonIdentForAktørId(aktørId.value()).orElseThrow());
                AKTØR_FNR.put(aktørId.value(), fnr);
                FNR_AKTØR.put(fnr.value(), aktørId);
                return fnr;
            });
    }

    @Override
    public AktørId aktørId(Fødselsnummer fnr) {
        return Optional.ofNullable(FNR_AKTØR.get(fnr.value()))
            .orElseGet(() -> {
                var aktørId = new AktørId(hentAktørIdForPersonIdent(fnr.value()).orElseThrow());
                FNR_AKTØR.put(fnr.value(), aktørId);
                return aktørId;
            });
    }

    @Override
    public AdresseBeskyttelse adresseBeskyttelse(Fødselsnummer fnr) throws BrukerIkkeFunnetIPdlException {
        if (FNR_ADRESSE.get(fnr.value()) != null) {
            return FNR_ADRESSE.get(fnr.value());
        }
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
        var beskyttelse = new AdresseBeskyttelse(gradering);
        FNR_ADRESSE.put(fnr.value(), beskyttelse);
        return beskyttelse;
    }

    @Override
    public String navn(String ident) {
        if (IDENT_NAVN.get(ident) != null) {
            return IDENT_NAVN.get(ident);
        }
        var request = new HentPersonQueryRequest();
        request.setIdent(ident);
        var projection = new PersonResponseProjection()
            .navn(new NavnResponseProjection().fornavn().mellomnavn().etternavn());
        var person = hentPerson(request, projection, true);

        var navn = Optional.ofNullable(person)
            .flatMap(PersonMappers::mapNavn)
            .orElse("Ukjent person");
        IDENT_NAVN.put(ident, navn);
        return navn;
    }

    @Override
    public boolean barnHarDisseForeldrene(Fødselsnummer barn, Fødselsnummer mor, Fødselsnummer annenForelder) {
        var request = new HentPersonQueryRequest();
        request.setIdent(barn.value());
        var projection = new PersonResponseProjection()
            .forelderBarnRelasjon(new ForelderBarnRelasjonResponseProjection().relatertPersonsIdent().relatertPersonsRolle());
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

    public static AdresseBeskyttelse.Gradering tilGradering(AdressebeskyttelseGradering adressebeskyttelseGradering) {
        if (adressebeskyttelseGradering == null) {
            return AdresseBeskyttelse.Gradering.UGRADERT;
        }
        return switch (adressebeskyttelseGradering) {
            case STRENGT_FORTROLIG_UTLAND, STRENGT_FORTROLIG, FORTROLIG -> AdresseBeskyttelse.Gradering.GRADERT;
            case UGRADERT -> AdresseBeskyttelse.Gradering.UGRADERT;
        };
    }

}
