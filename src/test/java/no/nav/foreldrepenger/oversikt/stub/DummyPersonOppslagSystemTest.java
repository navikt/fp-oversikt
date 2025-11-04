package no.nav.foreldrepenger.oversikt.stub;

import java.util.Set;

import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.saker.BrukerIkkeFunnetIPdlException;
import no.nav.foreldrepenger.oversikt.saker.PersonOppslagSystem;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.AdresseBeskyttelse;

public record DummyPersonOppslagSystemTest(AdresseBeskyttelse adresseBeskyttelse, BrukerIkkeFunnetIPdlException thrownException) implements PersonOppslagSystem {

    public static DummyPersonOppslagSystemTest annenpartUbeskyttetAdresse() {
        return new DummyPersonOppslagSystemTest(new AdresseBeskyttelse(Set.of()), null);
    }

    public static DummyPersonOppslagSystemTest annenbrukerBeskyttetAdresse() {
        return new DummyPersonOppslagSystemTest(new AdresseBeskyttelse(Set.of(AdresseBeskyttelse.Gradering.GRADERT)), null);
    }

    public static PersonOppslagSystem annenbrukerBeskyttetAdresse(BrukerIkkeFunnetIPdlException thrownException) {
        return new DummyPersonOppslagSystemTest(new AdresseBeskyttelse(Set.of()), thrownException);
    }

    @Override
    public AktørId aktørId(Fødselsnummer fnr) {
        return new AktørId(fnr.value());
    }

    @Override
    public Fødselsnummer fødselsnummer(AktørId aktørId) {
        return new Fødselsnummer(aktørId.value());
    }

    @Override
    public AdresseBeskyttelse adresseBeskyttelse(Fødselsnummer fnr) throws BrukerIkkeFunnetIPdlException {
        if (thrownException != null) {
            throw thrownException;
        }
        return adresseBeskyttelse;
    }

    @Override
    public String navn(String ident) {
        return "Navn";
    }

    @Override
    public boolean barnHarDisseForeldrene(Fødselsnummer barn, Fødselsnummer mor, Fødselsnummer annenForelder) {
        return false;
    }

}
