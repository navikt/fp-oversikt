package no.nav.foreldrepenger.oversikt.aareg;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.fpsak.tidsserie.LocalDateInterval;


@ApplicationScoped
public class ArbeidsforholdTjeneste {

    private AaregRestKlient aaregRestKlient;

    public ArbeidsforholdTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidsforholdTjeneste(AaregRestKlient aaregRestKlient) {
        this.aaregRestKlient = aaregRestKlient;
    }

    public Map<ArbeidsforholdIdentifikator, List<Arbeidsforhold>> finnArbeidsforholdForIdentIPerioden(Fødselsnummer ident, LocalDate fom, LocalDate tom) {
        return aaregRestKlient.finnArbeidsforholdForArbeidstaker(ident.value(), fom, tom, true).stream()
            .map(arbeidsforhold -> mapArbeidsforholdRSTilDto(arbeidsforhold, fom, tom))
            .collect(Collectors.groupingBy(Arbeidsforhold::arbeidsforholdIdentifikator));
    }


    private Arbeidsforhold mapArbeidsforholdRSTilDto(ArbeidsforholdRS arbeidsforhold, LocalDate fom, LocalDate tom) {
        var intervall = new LocalDateInterval(safeFom(fom), safeTom(tom));
        var arbeidsavtaler = arbeidsforhold.arbeidsavtaler()
            .stream()
            .map(this::byggArbeidsavtaleRS)
            .filter(av -> overlapperMedIntervall(av, intervall))
            .toList();

        var permisjoner = arbeidsforhold.getPermisjonPermitteringer().stream().map(this::byggPermisjonRS).toList();
        var ansettelsesPeriode = byggAnsettelsesPeriodeRS(arbeidsforhold);
        return new Arbeidsforhold(utledArbeidsgiverRS(arbeidsforhold),
            ansettelsesPeriode,
            arbeidsavtaler,
            permisjoner);
    }


    private ArbeidsforholdIdentifikator utledArbeidsgiverRS(ArbeidsforholdRS arbeidsforhold) {
        // Forenklet oppgjørsordning fra persom har ikke arbeidsforholdId
        return switch (arbeidsforhold.arbeidsgiver().type()) {
            case Person -> new ArbeidsforholdIdentifikator(arbeidsforhold.arbeidsgiver().aktoerId(),
                UUID.nameUUIDFromBytes(arbeidsforhold.type().getOffisiellKode().getBytes(StandardCharsets.UTF_8)).toString(), arbeidsforhold.type());
            case Organisasjon -> new ArbeidsforholdIdentifikator(arbeidsforhold.arbeidsgiver().organisasjonsnummer(),
                arbeidsforhold.arbeidsforholdId(), arbeidsforhold.type());
        };
    }

    private LocalDateInterval byggAnsettelsesPeriodeRS(ArbeidsforholdRS arbeidsforhold) {
        var ansettelseFom = safeFom(arbeidsforhold.ansettelsesperiode().periode().fom());
        var ansettelseTom = safeTom(arbeidsforhold.ansettelsesperiode().periode().tom());
        return new LocalDateInterval(ansettelseFom, ansettelseTom);
    }

    private Arbeidsavtale byggArbeidsavtaleRS(ArbeidsforholdRS.ArbeidsavtaleRS arbeidsavtale) {
        var stillingsprosent = Stillingsprosent.arbeid(safeProsent(arbeidsavtale.stillingsprosent()));
        var arbeidsavtaleFom = safeFom(arbeidsavtale.gyldighetsperiode().fom());
        var arbeidsavtaleTom = safeTom(arbeidsavtale.gyldighetsperiode().tom());
        var periode = new LocalDateInterval(arbeidsavtaleFom, arbeidsavtaleTom);
        return new Arbeidsavtale(periode, stillingsprosent);
    }

    private Permisjon byggPermisjonRS(ArbeidsforholdRS.PermisjonPermitteringRS permisjonPermitteringRS) {
        var permisjonprosent = Stillingsprosent.arbeid(safeProsent(permisjonPermitteringRS.prosent()));
        var permisjonFom = safeFom(permisjonPermitteringRS.periode().fom());
        var permisjonTom = safeTom(permisjonPermitteringRS.periode().tom());
        var periode = new LocalDateInterval(permisjonFom, permisjonTom);
        return new Permisjon(periode, permisjonprosent, permisjonPermitteringRS.type());
    }

    private boolean overlapperMedIntervall(Arbeidsavtale av, LocalDateInterval intervall) {
        return intervall.overlaps(av.arbeidsavtalePeriode());
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
