package no.nav.foreldrepenger.oversikt.innhenting.tilbakekreving;

import java.util.Optional;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

public interface FptilbakeTjeneste {

    Optional<Tilbakekreving> hent(Saksnummer saksnummer);
}
