package no.nav.foreldrepenger.oversikt.stub;

import java.util.UUID;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;

public record DummyInnloggetTestbruker(AktørId aktørId, Fødselsnummer fødselsnummer, boolean erMyndig) implements InnloggetBruker {

    public static DummyInnloggetTestbruker myndigInnloggetBruker(AktørId aktørId) {
        return new DummyInnloggetTestbruker(aktørId, new Fødselsnummer(UUID.randomUUID().toString()), true);
    }

    public static DummyInnloggetTestbruker myndigInnloggetBruker() {
        return new DummyInnloggetTestbruker(AktørId.dummy(), new Fødselsnummer(UUID.randomUUID().toString()), true);
    }

    public static DummyInnloggetTestbruker umyndigInnloggetBruker() {
        return new DummyInnloggetTestbruker(AktørId.dummy(), new Fødselsnummer(UUID.randomUUID().toString()), false);
    }


}
