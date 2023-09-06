package no.nav.foreldrepenger.oversikt.innhenting.tilbakekreving;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

import java.util.Optional;

public interface FptilbakeTjeneste {

    Optional<Tilbakekreving> hent(Saksnummer saksnummer);
}
