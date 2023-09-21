package no.nav.foreldrepenger.oversikt.stub;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;

public record DummyInnloggetTestbruker(AktørId aktørId, Fødselsnummer fødselsnummer, boolean erMyndig) implements InnloggetBruker {

    private static final Fødselsnummer DUMMY_FNR = new Fødselsnummer("11111111111");

    public static DummyInnloggetTestbruker myndigInnloggetBruker() {
        return new DummyInnloggetTestbruker(AktørId.dummy(), DUMMY_FNR,true);
    }

    public static DummyInnloggetTestbruker umyndigInnloggetBruker() {
        return new DummyInnloggetTestbruker(AktørId.dummy(), DUMMY_FNR,false);
    }


}
