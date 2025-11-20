package no.nav.foreldrepenger.oversikt.domene.beregning;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

import java.util.Set;

public interface BeregningRepository {
    void lagre(Saksnummer saksnummer, Set<Beregning> inntektsmeldinger);

    Set<Beregning> hentFor(Set<Saksnummer> saksnummer);

    void slett(Saksnummer saksnummer);
}
