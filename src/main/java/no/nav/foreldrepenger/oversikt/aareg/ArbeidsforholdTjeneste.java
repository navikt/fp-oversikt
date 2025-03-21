package no.nav.foreldrepenger.oversikt.aareg;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.fpsak.tidsserie.LocalDateInterval;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


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
        var intervall = new LocalDateInterval(fom, tom != null ? tom : LocalDate.MAX);
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
        if (ArbeidsforholdRS.OpplysningspliktigType.Person.equals(arbeidsforhold.arbeidsgiver().type())) {
            final var uuid = UUID.nameUUIDFromBytes(arbeidsforhold.type().getBytes(StandardCharsets.UTF_8));
            return new ArbeidsforholdIdentifikator(arbeidsforhold.arbeidsgiver().aktoerId(), arbeidsforhold.type(), uuid.toString());
        } else if (ArbeidsforholdRS.OpplysningspliktigType.Organisasjon.equals(arbeidsforhold.arbeidsgiver().type())) {
            return new ArbeidsforholdIdentifikator(arbeidsforhold.arbeidsgiver().organisasjonsnummer(),
                arbeidsforhold.type(), arbeidsforhold.arbeidsforholdId());
        } else {
            throw new IllegalArgumentException("Mangler arbeidsgivertype");
        }
    }

    private LocalDateInterval byggAnsettelsesPeriodeRS(ArbeidsforholdRS arbeidsforhold) {
        var ansettelseFom = arbeidsforhold.ansettelsesperiode().periode().fom();
        var ansettelseTom = arbeidsforhold.ansettelsesperiode().periode().tom();
        return new LocalDateInterval(ansettelseFom, ansettelseTom != null ? ansettelseTom : LocalDate.MAX);
    }

    private Arbeidsavtale byggArbeidsavtaleRS(ArbeidsforholdRS.ArbeidsavtaleRS arbeidsavtale) {
        var stillingsprosent = Stillingsprosent.normaliserStillingsprosentArbeid(arbeidsavtale.stillingsprosent());
        var arbeidsavtaleFom = arbeidsavtale.gyldighetsperiode().fom();
        var arbeidsavtaleTom = arbeidsavtale.gyldighetsperiode().tom();
        var periode = new LocalDateInterval(arbeidsavtaleFom, arbeidsavtaleTom != null ? arbeidsavtaleTom : LocalDate.MAX);
        return new Arbeidsavtale(periode, stillingsprosent);
    }

    private Permisjon byggPermisjonRS(ArbeidsforholdRS.PermisjonPermitteringRS permisjonPermitteringRS) {
        var permisjonprosent = Stillingsprosent.normaliserStillingsprosentArbeid(permisjonPermitteringRS.prosent());
        var permisjonFom = permisjonPermitteringRS.periode().fom();
        var permisjonTom = permisjonPermitteringRS.periode().tom();
        var periode = new LocalDateInterval(permisjonFom, permisjonTom != null ? permisjonTom : LocalDate.MAX);
        return new Permisjon(periode, permisjonprosent, permisjonPermitteringRS.type());
    }

    private boolean overlapperMedIntervall(Arbeidsavtale av, LocalDateInterval intervall) {
        return intervall.overlaps(av.arbeidsavtalePeriode());
    }

}
