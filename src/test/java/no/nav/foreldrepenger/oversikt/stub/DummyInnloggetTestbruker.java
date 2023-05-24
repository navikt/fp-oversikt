package no.nav.foreldrepenger.oversikt.stub;

import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;

public record DummyInnloggetTestbruker(AktørId aktørId, boolean erMyndig) implements InnloggetBruker {
    public static DummyInnloggetTestbruker myndigInnloggetBruker() {
        return new DummyInnloggetTestbruker(AktørId.dummy(), true);
    }

    public static DummyInnloggetTestbruker umyndigInnloggetBruker() {
        return new DummyInnloggetTestbruker(AktørId.dummy(), false);
    }
}
