package no.nav.foreldrepenger.oversikt.stub;

import java.util.Set;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.saker.PersonOppslagSystem;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.AdresseBeskyttelse;

public record DummyPersonOppslagSystemTest(AdresseBeskyttelse adresseBeskyttelse) implements PersonOppslagSystem {

    public static DummyPersonOppslagSystemTest annenpartUbeskyttetAdresse() {
        return new DummyPersonOppslagSystemTest(new AdresseBeskyttelse(Set.of()));
    }

    public static DummyPersonOppslagSystemTest annenbrukerBeskyttetAdresse() {
        return new DummyPersonOppslagSystemTest(new AdresseBeskyttelse(Set.of(AdresseBeskyttelse.Gradering.GRADERT)));
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
    public AdresseBeskyttelse adresseBeskyttelse(Fødselsnummer fnr) {
        return adresseBeskyttelse;
    }

}
