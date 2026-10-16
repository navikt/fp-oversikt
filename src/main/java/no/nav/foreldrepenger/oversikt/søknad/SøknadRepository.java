package no.nav.foreldrepenger.oversikt.søknad;

import java.util.Optional;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

public interface SøknadRepository {

    void lagre(Saksnummer saksnummer, Søknad søknad);

    Optional<Søknad> hentAktivFor(Saksnummer saksnummer);
}
