package no.nav.foreldrepenger.oversikt.domene.tilbakekreving;

import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

public interface TilbakekrevingRepository {

    void lagre(Tilbakekreving tilbakekreving);

    Set<Tilbakekreving> hentFor(Set<Saksnummer> saksnummer);

    Optional<Tilbakekreving> hentFor(Saksnummer saksnummer);

    void slett(Saksnummer saksnummer);
}
