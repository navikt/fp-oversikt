package no.nav.foreldrepenger.oversikt.innhenting.inntektsmelding;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger.Inntektsmelding;
import no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger.InntektsmeldingerRepository;

import java.util.List;
import java.util.Set;

@ApplicationScoped
public class InntektsmeldingTjeneste {

    private InntektsmeldingerRepository inntektsmeldingerRepository;

    @Inject
    public InntektsmeldingTjeneste(InntektsmeldingerRepository inntektsmeldingerRepository) {
        this.inntektsmeldingerRepository = inntektsmeldingerRepository;
    }

    public InntektsmeldingTjeneste() {
        // CDI
    }

    public List<InntektsmeldingDto> inntektsmeldinger(Saksnummer saksnummer) {
        return inntektsmeldingerRepository.hentFor(Set.of(saksnummer)).stream()
                .map(InntektsmeldingTjeneste::tilInntektsmeldingDto)
                .toList();
    }

    private static InntektsmeldingDto tilInntektsmeldingDto(Inntektsmelding inntektsmelding) {
        return new InntektsmeldingDto(inntektsmelding.mottattTidspunkt() == null ? inntektsmelding.innsendingstidspunkt() : inntektsmelding.mottattTidspunkt());
    }
}
