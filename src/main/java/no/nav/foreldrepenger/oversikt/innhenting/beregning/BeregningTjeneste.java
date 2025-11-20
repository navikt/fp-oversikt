package no.nav.foreldrepenger.oversikt.innhenting.beregning;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger.InntektsmeldingerRepository;

public class BeregningTjeneste {
    private InntektsmeldingerRepository inntektsmeldingerRepository;

    @Inject
    public InntektsmeldingTjeneste(InntektsmeldingerRepository inntektsmeldingerRepository) {
        this.inntektsmeldingerRepository = inntektsmeldingerRepository;
    }

    InntektsmeldingTjeneste() {
        // CDI
    }

    public Object finnBeregning(@Valid @NotNull Saksnummer saksnummer) {
    }
}
