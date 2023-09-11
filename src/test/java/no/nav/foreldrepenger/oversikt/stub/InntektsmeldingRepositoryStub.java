package no.nav.foreldrepenger.oversikt.stub;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger.Inntektsmelding;
import no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger.InntektsmeldingerRepository;

public class InntektsmeldingRepositoryStub implements InntektsmeldingerRepository {

    private final Map<Saksnummer, Set<Inntektsmelding>> inntektsmeldinger = new ConcurrentHashMap<>();

    @Override
    public void lagre(Saksnummer saksnummer, Set<Inntektsmelding> im) {
        inntektsmeldinger.put(saksnummer, im);
    }

    @Override
    public Set<Inntektsmelding> hentFor(Set<Saksnummer> saksnummer) {
        var IM = new java.util.HashSet<Inntektsmelding>();
        for (var sak: saksnummer) {
            var inntektsmeldingerPåSak = inntektsmeldinger.get(sak);
            if (inntektsmeldingerPåSak != null) {
                IM.addAll(inntektsmeldingerPåSak);
            }
        }
        return IM;
    }

    @Override
    public void slett(Saksnummer saksnummer) {
        inntektsmeldinger.remove(saksnummer);
    }
}
