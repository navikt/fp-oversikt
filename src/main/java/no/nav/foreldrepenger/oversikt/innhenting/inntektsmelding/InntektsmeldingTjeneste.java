package no.nav.foreldrepenger.oversikt.innhenting.inntektsmelding;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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

    List<InntektsmeldingDto> inntektsmeldinger(Saksnummer saksnummer) {
        return inntektsmeldingerRepository.hentFor(Set.of(saksnummer)).stream().map(InntektsmeldingTjeneste::tilInntektsmeldingDto).toList();
    }

    private static InntektsmeldingDto tilInntektsmeldingDto(Inntektsmelding inntektsmelding) {
        if (inntektsmelding instanceof InntektsmeldingV2 inntektsmeldingV2) {
            var naturalytelser = inntektsmeldingV2.bortfalteNaturalytelser()
                .stream()
                .map(n -> new InntektsmeldingDto.NaturalYtelse(n.fomDato(), n.tomDato(), n.beloepPerMnd(), n.type()))
                .toList();
            var refusjon = inntektsmeldingV2.refusjonsperioder()
                .stream()
                .map(r -> new InntektsmeldingDto.Refusjon(r.fomDato(), r.refusjonsbeløpMnd()))
                .toList();
            return new InntektsmeldingDto(2, inntektsmeldingV2.erAktiv(), inntektsmeldingV2.stillingsprosent(), inntektsmeldingV2.inntektPrMnd(), inntektsmeldingV2.refusjonPrMnd(),
                inntektsmeldingV2.arbeidsgiverNavn(), inntektsmeldingV2.journalpostId(), inntektsmeldingV2.kontaktpersonNavn(),
                inntektsmeldingV2.kontaktpersonNummer(), inntektsmeldingV2.innsendingstidspunkt(), inntektsmeldingV2.mottattTidspunkt(),
                inntektsmeldingV2.startDatoPermisjon(), naturalytelser, refusjon);
        }
        if (inntektsmelding instanceof InntektsmeldingV1 inntektsmeldingV1) {
            var mottatTidspunkt =
                inntektsmeldingV1.mottattTidspunkt() == null ? inntektsmeldingV1.innsendingstidspunkt() : inntektsmeldingV1.mottattTidspunkt();
            // TODO: finn ut hvilke tidspunkt som faktisk trengs
            return new InntektsmeldingDto(1, false, null, null, null, null, null, null, null, null, mottatTidspunkt, null, Collections.emptyList(),
                Collections.emptyList());
        }

        throw new IllegalStateException("Inntektsmelding er hverken av type 1 eller 2");
    }
}
