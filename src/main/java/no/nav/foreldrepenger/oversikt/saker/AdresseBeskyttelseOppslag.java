package no.nav.foreldrepenger.oversikt.saker;

import java.util.Optional;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.AdresseBeskyttelse;

public interface AdresseBeskyttelseOppslag {

    Optional<AdresseBeskyttelse> adresseBeskyttelse(Fødselsnummer fnr);
}
