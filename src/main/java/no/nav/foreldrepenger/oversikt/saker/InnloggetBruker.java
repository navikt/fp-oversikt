package no.nav.foreldrepenger.oversikt.saker;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;

public interface InnloggetBruker {

    AktørId aktørId();

    Fødselsnummer fødselsnummer();

    boolean erMyndig();

}
