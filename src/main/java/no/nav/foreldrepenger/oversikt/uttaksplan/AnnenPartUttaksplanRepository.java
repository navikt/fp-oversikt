package no.nav.foreldrepenger.oversikt.uttaksplan;

import java.util.Optional;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

public interface AnnenPartUttaksplanRepository {

    void lagre(Saksnummer saksnummer, AnnenPartUttaksplan uttaksplan);

    Optional<AnnenPartUttaksplan> hentFor(Saksnummer saksnummer);
}
