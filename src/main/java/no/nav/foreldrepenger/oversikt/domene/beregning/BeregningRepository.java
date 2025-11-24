package no.nav.foreldrepenger.oversikt.domene.beregning;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

import java.util.Optional;
import java.util.Set;

public interface BeregningRepository {
    void lagre(Saksnummer saksnummer, Beregning inntektsmeldinger);

    Optional<Beregning> hentFor(Saksnummer saksnummer);

    void slett(Saksnummer saksnummer);
}
