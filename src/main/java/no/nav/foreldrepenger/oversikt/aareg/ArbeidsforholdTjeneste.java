package no.nav.foreldrepenger.oversikt.aareg;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;


@ApplicationScoped
public class ArbeidsforholdTjeneste {

    private static final Period TID_TILBAKE_ARBEID = Period.ofYears(3);
    private static final Period TID_TILBAKE_FRILANS = Period.ofMonths(6);
    private static final Period TID_FRAMOVER = Period.ofMonths(3);

    private AaregRestKlient aaregRestKlient;

    public ArbeidsforholdTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidsforholdTjeneste(AaregRestKlient aaregRestKlient) {
        this.aaregRestKlient = aaregRestKlient;
    }

    public List<Arbeidsforhold> finnAktiveArbeidsforholdForIdent(Fødselsnummer ident) {
        var spørFra = LocalDate.now().minus(TID_TILBAKE_ARBEID);
        var spørTil = LocalDate.now().plus(TID_FRAMOVER);
        return aaregRestKlient.finnArbeidsforholdForArbeidstaker(ident.value(), spørFra, spørTil, false).stream()
            .map(arbeidsforhold -> mapArbeidsforholdRSTilDto(arbeidsforhold, spørFra, spørTil))
            .toList();
    }

    public List<Arbeidsforhold> finnFrilansForIdent(Fødselsnummer ident) {
        var spørFra = LocalDate.now().minus(TID_TILBAKE_FRILANS);
        var spørTil = LocalDate.now().plus(TID_FRAMOVER);
        return aaregRestKlient.finnArbeidsforholdForArbeidstaker(ident.value(), spørFra, spørTil,
                Optional.of(ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER), true).stream()
            .map(arbeidsforhold -> mapArbeidsforholdRSTilDto(arbeidsforhold, spørFra, spørTil))
            .toList();
    }

    public List<Arbeidsforhold> finnAlleArbeidsforholdForIdentIPerioden(Fødselsnummer ident, LocalDate fom, LocalDate tom) {
        return aaregRestKlient.finnArbeidsforholdForArbeidstaker(ident.value(), fom, tom, true).stream()
            .map(arbeidsforhold -> mapArbeidsforholdRSTilDto(arbeidsforhold, fom, tom))
            .toList();
    }

    private Arbeidsforhold mapArbeidsforholdRSTilDto(ArbeidsforholdRS arbeidsforhold, LocalDate fom, LocalDate tom) {
        var innhentingsIntervall = new LocalDateInterval(safeFom(fom), safeTom(tom));

        var ansettelsesPeriode = byggAnsettelsesPeriodeRS(arbeidsforhold);

        var arbeidsavtaler = arbeidsforhold.arbeidsavtaler()
            .stream()
            .filter(a -> a.stillingsprosent() != null) // De blir uansett forkastet senere
            .map(this::byggArbeidsavtaleRS)
            .filter(av -> overlapperArbeidMedIntervall(av, innhentingsIntervall))
            .collect(Collectors.collectingAndThen(Collectors.toList(), LocalDateTimeline::new))
            .intersection(ansettelsesPeriode);

        var permisjoner = arbeidsforhold.getPermisjonPermitteringer().stream()
            .filter(p -> p.prosent() != null) // De blir uansett forkastet senere
            .map(this::byggPermisjonRS)
            .filter(perm -> overlapperPermisjonMedIntervall(perm, innhentingsIntervall))
            .collect(Collectors.collectingAndThen(Collectors.toList(),
                datoSegmenter -> new LocalDateTimeline<>(datoSegmenter, StandardCombinators::concatLists)))
            .intersection(ansettelsesPeriode);


        return new Arbeidsforhold(utledArbeidsgiverRS(arbeidsforhold), ansettelsesPeriode, arbeidsavtaler, permisjoner);
    }


    private ArbeidsforholdIdentifikator utledArbeidsgiverRS(ArbeidsforholdRS arbeidsforhold) {
        // Forenklet oppgjørsordning fra persom har ikke arbeidsforholdId
        return switch (arbeidsforhold.arbeidsgiver().type()) {
            case PERSON -> new ArbeidsforholdIdentifikator(arbeidsforhold.arbeidsgiver().offentligIdent(),
                UUID.nameUUIDFromBytes(arbeidsforhold.type().getOffisiellKode().getBytes(StandardCharsets.UTF_8)).toString(), arbeidsforhold.type());
            case ORGANISASJON -> new ArbeidsforholdIdentifikator(arbeidsforhold.arbeidsgiver().organisasjonsnummer(),
                arbeidsforhold.arbeidsforholdId(), arbeidsforhold.type());
        };
    }

    private LocalDateInterval byggAnsettelsesPeriodeRS(ArbeidsforholdRS arbeidsforhold) {
        var ansettelseFom = safeFom(arbeidsforhold.ansettelsesperiode().periode().fom());
        var ansettelseTom = safeTom(arbeidsforhold.ansettelsesperiode().periode().tom());
        return new LocalDateInterval(ansettelseFom, ansettelseTom);
    }

    private LocalDateSegment<Stillingsprosent> byggArbeidsavtaleRS(ArbeidsforholdRS.ArbeidsavtaleRS arbeidsavtale) {
        var stillingsprosent = Stillingsprosent.arbeid(safeProsent(arbeidsavtale.stillingsprosent()));
        var arbeidsavtaleFom = safeFom(arbeidsavtale.gyldighetsperiode().fom());
        var arbeidsavtaleTom = safeTom(arbeidsavtale.gyldighetsperiode().tom());
        var periode = new LocalDateInterval(arbeidsavtaleFom, arbeidsavtaleTom);
        return new LocalDateSegment<>(periode, stillingsprosent);
    }

    private LocalDateSegment<List<Permisjon>> byggPermisjonRS(ArbeidsforholdRS.PermisjonPermitteringRS permisjonPermitteringRS) {
        var permisjonprosent = Stillingsprosent.arbeid(safeProsent(permisjonPermitteringRS.prosent()));
        var permisjonFom = safeFom(permisjonPermitteringRS.periode().fom());
        var permisjonTom = safeTom(permisjonPermitteringRS.periode().tom());
        var periode = new LocalDateInterval(permisjonFom, permisjonTom);
        return new LocalDateSegment<>(periode, List.of(new Permisjon(permisjonprosent, permisjonPermitteringRS.type())));
    }

    private boolean overlapperArbeidMedIntervall(LocalDateSegment<Stillingsprosent> arbeid, LocalDateInterval innhentingsIntervall) {
        return innhentingsIntervall.overlaps(arbeid.getLocalDateInterval());
    }

    private boolean overlapperPermisjonMedIntervall(LocalDateSegment<List<Permisjon>> perm, LocalDateInterval innhentingsIntervall) {
        return innhentingsIntervall.overlaps(perm.getLocalDateInterval());
    }

    private LocalDate safeFom(LocalDate fom) {
        return fom == null ? LocalDate.MIN : fom;
    }

    private LocalDate safeTom(LocalDate tom) {
        return tom == null ? LocalDate.MAX : tom;
    }

    private BigDecimal safeProsent(BigDecimal prosent) {
        return prosent == null ? BigDecimal.ZERO : prosent;
    }

}
