package no.nav.foreldrepenger.oversikt.oppslag;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.arbeid.Arbeidsforhold;
import no.nav.foreldrepenger.oversikt.arbeid.ArbeidsforholdIdentifikator;
import no.nav.foreldrepenger.oversikt.arbeid.ArbeidsforholdTjeneste;
import no.nav.foreldrepenger.oversikt.arbeid.EksternArbeidsforholdDto;
import no.nav.foreldrepenger.oversikt.arbeid.Stillingsprosent;
import no.nav.foreldrepenger.oversikt.integrasjoner.ereg.VirksomhetTjeneste;
import no.nav.foreldrepenger.oversikt.saker.PersonOppslagSystem;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;


/**
 * Tjeneste for å hente brukers arbeidsforhold på et gitt tidspunkt fra Aa-registeret.
 */
@ApplicationScoped
public class MineArbeidsforholdTjeneste {
    private ArbeidsforholdTjeneste arbeidsforholdTjeneste;
    private PersonOppslagSystem personOppslagSystem;
    private VirksomhetTjeneste virksomhetTjeneste;

    public MineArbeidsforholdTjeneste() {
        //CDI
    }

    @Inject
    public MineArbeidsforholdTjeneste(ArbeidsforholdTjeneste arbeidsforholdTjeneste,
                                      PersonOppslagSystem personOppslagSystem,
                                      VirksomhetTjeneste virksomhetTjeneste) {
        this.arbeidsforholdTjeneste = arbeidsforholdTjeneste;
        this.personOppslagSystem = personOppslagSystem;
        this.virksomhetTjeneste = virksomhetTjeneste;
    }

    public List<EksternArbeidsforholdDto> brukersArbeidsforhold(Fødselsnummer brukerFødselsnummer) {
        // Slår opp i Aa-register, velger typer arbeidsforhold som er relevante og mapper om til eksternt format (med navn)
        var alleAktiveArbeidsforhold = arbeidsforholdTjeneste.finnAktiveArbeidsforholdForIdent(brukerFødselsnummer);
        return slåSammenLikeArbeidsforhold(alleAktiveArbeidsforhold).stream()
            .sorted(Comparator.comparing(EksternArbeidsforholdDto::arbeidsgiverNavn))
            .toList();
    }

    private List<EksternArbeidsforholdDto> slåSammenLikeArbeidsforhold(List<Arbeidsforhold> alleArbeidsforhold) {
        return alleArbeidsforhold.stream()
            .collect(Collectors.groupingBy(Arbeidsforhold::arbeidsforholdIdentifikator))
            .values()
            .stream()
            .map(this::slåSammeArbeidsforholdFraSammeArbeidsgiver)
            .flatMap(Collection::stream)
            .toList();
    }

    private List<EksternArbeidsforholdDto> slåSammeArbeidsforholdFraSammeArbeidsgiver(List<Arbeidsforhold> arbeidsforholdListe) {
        var arbeidsforholdIdentifikator = arbeidsforholdListe.getFirst().arbeidsforholdIdentifikator();
        return arbeidsforholdListe.stream()
            .map(a -> new LocalDateSegment<>(a.ansettelsesPeriode(), gjeldendeStillingsprosent(a)))
            .collect(Collectors.collectingAndThen(Collectors.toList(), LocalDateTimeline::new))
            .compress()
            .stream()
            .map(seg -> new EksternArbeidsforholdDto(
                arbeidsforholdIdentifikator.arbeidsgiver(),
                tilArbeidsgiverTypeFrontend(arbeidsforholdIdentifikator),
                arbeidsgiverNavn(arbeidsforholdIdentifikator),
                seg.getValue(),
                seg.getFom(),
                Optional.of(seg.getTom()).filter(d -> d.isBefore(LocalDate.MAX)).orElse(null)
            ))
            .toList();
    }

    public List<EksternArbeidsforholdDto> brukersFrilansoppdragSisteSeksMåneder(Fødselsnummer brukerFødselsnummer) {
        // Slår opp i Aa-register, velger typer arbeidsforhold som er relevante og mapper om til eksternt format (med navn)
        return arbeidsforholdTjeneste.finnFrilansForIdent(brukerFødselsnummer).stream()
            .map(this::tilEksternArbeidsforhold)
            .sorted(Comparator.comparing(EksternArbeidsforholdDto::arbeidsgiverNavn))
            .toList();
    }

    private EksternArbeidsforholdDto tilEksternArbeidsforhold(Arbeidsforhold a) {
        return new EksternArbeidsforholdDto(
                a.arbeidsforholdIdentifikator().arbeidsgiver(),
                tilArbeidsgiverTypeFrontend(a.arbeidsforholdIdentifikator()),
                arbeidsgiverNavn(a.arbeidsforholdIdentifikator()),
                gjeldendeStillingsprosent(a),
                a.ansettelsesPeriode().getFomDato(),
                Optional.of(a.ansettelsesPeriode().getTomDato()).filter(d -> d.isBefore(LocalDate.MAX)).orElse(null)
        );
    }

    private String arbeidsgiverNavn(ArbeidsforholdIdentifikator a) {
        if (VirksomhetTjeneste.erOrganisasjonsNummer(a.arbeidsgiver())) {
            return virksomhetTjeneste.hentOrganisasjonNavn(a.arbeidsgiver());
        } else if (erIdent(a.arbeidsgiver())) {
            return personOppslagSystem.navn(a.arbeidsgiver());
        } else {
            return a.arbeidsgiver();
        }
    }

    private static Stillingsprosent gjeldendeStillingsprosent(Arbeidsforhold arbeidsforhold) {
        if (arbeidsforhold.arbeidsavtaler().isEmpty()) {
            return Stillingsprosent.arbeid(BigDecimal.ZERO);
        }

        return arbeidsforhold.arbeidsavtaler().stream()
            .filter(s -> erGjeldende(s.getLocalDateInterval()))
            .map(LocalDateSegment::getValue)
            .findFirst()
            .orElseGet(() -> hentSistGjeldendeStillingsprsoent(arbeidsforhold));
    }

    private static Stillingsprosent hentSistGjeldendeStillingsprsoent(Arbeidsforhold arbeidsforhold) {
        if (arbeidsforhold.arbeidsavtaler().stream().allMatch(a -> a.getFom().isAfter(LocalDate.now()))) {
            // Tilkommet arbeidsforhold så bruker vi første stillingsprosent hvis flere
            return arbeidsforhold.arbeidsavtaler().stream()
                    .min(Comparator.comparing(LocalDateSegment::getFom))
                    .map(LocalDateSegment::getValue)
                    .orElseThrow();
        } else {
            // Arbeidsforhold som er avsluttet eller både frem og tilbake i tid så bruker vi siste gjeldende stillingsprosent
            return arbeidsforhold.arbeidsavtaler().stream()
                    .max(Comparator.comparing(LocalDateSegment::getFom))
                    .map(LocalDateSegment::getValue)
                    .orElseThrow();
        }
    }

    private static boolean erGjeldende(LocalDateInterval interval) {
        return interval.overlaps(new LocalDateInterval(LocalDate.now(), LocalDate.now()));
    }

    private static String tilArbeidsgiverTypeFrontend(ArbeidsforholdIdentifikator a) {
        if (VirksomhetTjeneste.erOrganisasjonsNummer(a.arbeidsgiver())) {
            return "orgnr";
        } else {
            return "fnr";
        }
    }

    private static boolean erIdent(String ident) {
        return ident != null && ident.matches("\\d{11}|\\d{13}");
    }

}
