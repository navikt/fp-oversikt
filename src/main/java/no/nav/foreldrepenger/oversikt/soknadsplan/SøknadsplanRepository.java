package no.nav.foreldrepenger.oversikt.soknadsplan;

import java.util.Optional;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

public interface SøknadsplanRepository {

    void lagre(Saksnummer saksnummer, Søknadsplan søknadsplan);

    Optional<Søknadsplan> hentAktivFor(Saksnummer saksnummer);
}
