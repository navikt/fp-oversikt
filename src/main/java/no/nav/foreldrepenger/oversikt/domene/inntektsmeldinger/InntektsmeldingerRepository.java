package no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger;

import java.util.Set;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

public interface InntektsmeldingerRepository {

    void lagre(Saksnummer saksnummer, Set<Inntektsmelding> inntektsmeldinger);

    Set<Inntektsmelding> hentFor(Set<Saksnummer> saksnummer);

    void slett(Saksnummer saksnummer);
}
