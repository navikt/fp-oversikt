package no.nav.foreldrepenger.oversikt.arbeid;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.integrasjoner.aareg.AaregRestKlient;
import no.nav.foreldrepenger.oversikt.integrasjoner.aareg.ArbeidType;
import no.nav.foreldrepenger.oversikt.integrasjoner.aareg.ArbeidsforholdRS;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;


@ApplicationScoped
public class ArbeidsforholdTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ArbeidsforholdTjeneste.class);

    private static final Period TID_TILBAKE_ARBEID = Period.ofYears(3);
    private static final Period TID_TILBAKE_FRILANS = Period.ofMonths(6);
    // private static final Period TID_FRAMOVER = Period.ofYears(3);

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
        var innhentingsIntervall = new LocalDateInterval(spørFra, LocalDate.MAX);
        return aaregRestKlient.finnArbeidsforholdForArbeidstaker(ident.value(), spørFra, null, false).stream()
            .map(arbeidsforhold -> mapArbeidsforholdRSTilDto(arbeidsforhold, innhentingsIntervall))
            .filter(a -> innhentingsIntervall.overlaps(a.ansettelsesPeriode()))
            .toList();
    }

    public List<Arbeidsforhold> finnFrilansForIdent(Fødselsnummer ident) {
        var spørFra = LocalDate.now().minus(TID_TILBAKE_FRILANS);
        var innhentingsIntervall = new LocalDateInterval(spørFra, LocalDate.MAX);
        return aaregRestKlient.finnArbeidsforholdForArbeidstaker(ident.value(), spørFra, null,
                Optional.of(ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER), true).stream()
            .map(arbeidsforhold -> mapArbeidsforholdRSTilDto(arbeidsforhold, innhentingsIntervall))
            .filter(a -> a.ansettelsesPeriode().overlaps(innhentingsIntervall))
            .toList();
    }

    public List<Arbeidsforhold> finnAlleArbeidsforholdForIdentIPerioden(Fødselsnummer ident, LocalDate fom, LocalDate tom) {
        var innhentingsIntervall = new LocalDateInterval(safeFom(fom), safeTom(tom));
        return aaregRestKlient.finnArbeidsforholdForArbeidstaker(ident.value(), fom, tom, true).stream()
            .map(arbeidsforhold -> mapArbeidsforholdRSTilDto(arbeidsforhold, innhentingsIntervall))
            .filter(a -> a.ansettelsesPeriode().overlaps(innhentingsIntervall))
            .toList();
    }

    private Arbeidsforhold mapArbeidsforholdRSTilDto(ArbeidsforholdRS arbeidsforhold, LocalDateInterval innhentingsIntervall) {

        var ansettelsesPeriode = byggAnsettelsesPeriodeRS(arbeidsforhold);

        LOG.info("Arbeidsavtaler for intervallet {} er {}", ansettelsesPeriode, arbeidsforhold.arbeidsavtaler());
        var arbeidsavtaler = arbeidsforhold.arbeidsavtaler().stream()
            .filter(a -> a.stillingsprosent() != null) // De blir uansett forkastet senere
            .map(this::byggArbeidsavtaleRS)
            .filter(av -> innhentingsIntervall.overlaps(av.getLocalDateInterval()))
            .collect(Collectors.collectingAndThen(Collectors.toList(), LocalDateTimeline::new))
            .intersection(ansettelsesPeriode);

        var permisjoner = arbeidsforhold.getPermisjonPermitteringer().stream()
            .filter(p -> p.prosent() != null) // De blir uansett forkastet senere
            .map(this::byggPermisjonRS)
            .filter(perm -> innhentingsIntervall.overlaps(perm.getLocalDateInterval()))
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
