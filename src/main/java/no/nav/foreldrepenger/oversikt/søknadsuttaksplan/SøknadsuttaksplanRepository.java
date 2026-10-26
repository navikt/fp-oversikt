package no.nav.foreldrepenger.oversikt.søknadsuttaksplan;

import java.util.Optional;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

public interface SøknadsuttaksplanRepository {

    void lagre(Saksnummer saksnummer, Søknadsuttaksplan søknadsuttaksplan);

    Optional<Søknadsuttaksplan> hentAktivFor(Saksnummer saksnummer);
}
