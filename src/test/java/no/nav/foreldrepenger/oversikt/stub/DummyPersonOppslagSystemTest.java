package no.nav.foreldrepenger.oversikt.stub;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.saker.PersonOppslagSystem;

public record DummyPersonOppslagSystemTest() implements PersonOppslagSystem {

    @Override
    public AktørId aktørId(Fødselsnummer fnr) {
        return new AktørId(fnr.value());
    }

    @Override
    public Fødselsnummer fødselsnummer(AktørId aktørId) {
        return new Fødselsnummer(aktørId.value());
    }

}
