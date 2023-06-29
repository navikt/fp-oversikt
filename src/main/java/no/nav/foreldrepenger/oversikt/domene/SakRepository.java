package no.nav.foreldrepenger.oversikt.domene;

import java.util.List;
import java.util.Optional;

public interface SakRepository {
    void lagre(Sak sak);

    List<Sak> hentFor(AktørId aktørId);

    Optional<Sak> hent(Saksnummer saksnummer);
}
