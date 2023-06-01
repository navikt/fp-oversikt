package no.nav.foreldrepenger.oversikt.saker;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;

public interface AdresseBeskyttelseOppslag {

    no.nav.foreldrepenger.oversikt.tilgangskontroll.AdresseBeskyttelse adresseBeskyttelse(Fødselsnummer fnr);
}
