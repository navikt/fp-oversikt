package no.nav.foreldrepenger.oversikt.stub;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger.Inntektsmelding;
import no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger.InntektsmeldingerRepository;

public class InntektsmeldingRepositoryStub implements InntektsmeldingerRepository {

    private final Map<Saksnummer, Set<Inntektsmelding>> inntektsmeldinger = new ConcurrentHashMap<>();

    @Override
    public void lagre(Saksnummer saksnummer, Set<Inntektsmelding> im) {
        inntektsmeldinger.put(saksnummer, new HashSet<>(im));
    }

    @Override
    public Set<Inntektsmelding> hentFor(Set<Saksnummer> saksnummer) {
        return saksnummer.stream().flatMap(s -> inntektsmeldinger.getOrDefault(s, Set.of()).stream()).collect(Collectors.toSet());
    }

    @Override
    public void slett(Saksnummer saksnummer) {
        inntektsmeldinger.remove(saksnummer);
    }
}
