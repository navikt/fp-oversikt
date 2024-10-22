package no.nav.foreldrepenger.oversikt.innhenting.inntektsmelding;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.common.innsyn.inntektsmelding.BortfaltNaturalytelse;
import no.nav.foreldrepenger.common.innsyn.inntektsmelding.FpOversiktInntektsmeldingDto;
import no.nav.foreldrepenger.common.innsyn.inntektsmelding.Refusjon;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger.Inntektsmelding;
import no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger.InntektsmeldingV1;
import no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger.InntektsmeldingV2;
import no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger.InntektsmeldingerRepository;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class InntektsmeldingTjeneste {

    private InntektsmeldingerRepository inntektsmeldingerRepository;

    @Inject
    public InntektsmeldingTjeneste(InntektsmeldingerRepository inntektsmeldingerRepository) {
        this.inntektsmeldingerRepository = inntektsmeldingerRepository;
    }

    InntektsmeldingTjeneste() {
        // CDI
    }

    List<FpOversiktInntektsmeldingDto> inntektsmeldinger(Saksnummer saksnummer) {
        return inntektsmeldingerRepository.hentFor(Set.of(saksnummer)).stream().map(InntektsmeldingTjeneste::tilInntektsmeldingDto).toList();
    }

    private static FpOversiktInntektsmeldingDto tilInntektsmeldingDto(Inntektsmelding inntektsmelding) {
        if (inntektsmelding instanceof InntektsmeldingV2 inntektsmeldingV2) {
            var naturalytelser = inntektsmeldingV2.bortfalteNaturalytelser()
                .stream()
                .map(n -> new BortfaltNaturalytelse(n.fomDato(), n.tomDato(), n.beløpPerMnd(), no.nav.foreldrepenger.common.innsyn.inntektsmelding.NaturalytelseType.valueOf(n.type())))
                .toList();
            var refusjon = inntektsmeldingV2.refusjonsperioder()
                .stream()
                .map(r -> new Refusjon(r.refusjonsbeløpMnd(), r.fomDato()))
                .toList();
            return new FpOversiktInntektsmeldingDto(2, inntektsmeldingV2.erAktiv(), inntektsmeldingV2.stillingsprosent(),
                inntektsmeldingV2.inntektPrMnd(), inntektsmeldingV2.refusjonPrMnd(), inntektsmeldingV2.arbeidsgiverNavn(),
                inntektsmeldingV2.arbeidsgiverIdent(), inntektsmeldingV2.journalpostId(), inntektsmeldingV2.mottattTidspunkt(), inntektsmeldingV2.startDatoPermisjon(), naturalytelser,
                refusjon);
        }
        if (inntektsmelding instanceof InntektsmeldingV1 inntektsmeldingV1) {
            var mottatTidspunkt =
                inntektsmeldingV1.mottattTidspunkt() == null ? inntektsmeldingV1.innsendingstidspunkt() : inntektsmeldingV1.mottattTidspunkt();
            return new FpOversiktInntektsmeldingDto(1, false, null, null, null, null, null, null, mottatTidspunkt, null, Collections.emptyList(),
                Collections.emptyList());
        }

        throw new IllegalStateException("Inntektsmelding er hverken av type 1 eller 2");
    }
}
