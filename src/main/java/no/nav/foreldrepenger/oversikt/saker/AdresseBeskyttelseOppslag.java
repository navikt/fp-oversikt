package no.nav.foreldrepenger.oversikt.saker;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.AdresseBeskyttelse;

public interface AdresseBeskyttelseOppslag {

    AdresseBeskyttelse adresseBeskyttelse(Fødselsnummer fnr);
}
