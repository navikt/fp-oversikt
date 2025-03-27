package no.nav.foreldrepenger.oversikt.aareg;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.ereg.VirksomhetTjeneste;
import no.nav.foreldrepenger.oversikt.saker.PersonOppslagSystem;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;


/**
 * Tjeneste for å hente brukers arbeidsforhold på et gitt tidspunkt fra Aa-registeret.
 */
@ApplicationScoped
public class MineArbeidsforholdTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(MineArbeidsforholdTjeneste.class);

    private ArbeidsforholdTjeneste arbeidsforholdTjeneste;
    private PersonOppslagSystem personOppslagSystem;
    private VirksomhetTjeneste virksomhetTjeneste;

    public MineArbeidsforholdTjeneste() {
        //CDI
    }

    @Inject
    public MineArbeidsforholdTjeneste(ArbeidsforholdTjeneste arbeidsforholdTjeneste,
                                      PersonOppslagSystem personOppslagSystem, VirksomhetTjeneste virksomhetTjeneste) {
        this.arbeidsforholdTjeneste = arbeidsforholdTjeneste;
        this.personOppslagSystem = personOppslagSystem;

        this.virksomhetTjeneste = virksomhetTjeneste;
    }

    public List<EksternArbeidsforhold> brukersArbeidsforhold(Fødselsnummer brukerFødselsnummer) {
        // Slår opp i Aa-register, velger typer arbeidsforhold som er relevante og mapper om til eksternt format (med navn)
        return arbeidsforholdTjeneste.finnAktiveArbeidsforholdForIdent(brukerFødselsnummer).stream()
            .map(this::tilEksternArbeidsforhold)
            .sorted(Comparator.comparing(EksternArbeidsforhold::arbeidsgiverNavn))
            .toList();
    }

    public List<EksternArbeidsforhold> brukersFrilansoppdragSisteSeksMåneder(Fødselsnummer brukerFødselsnummer) {
        // Slår opp i Aa-register, velger typer arbeidsforhold som er relevante og mapper om til eksternt format (med navn)
        return arbeidsforholdTjeneste.finnFrilansForIdent(brukerFødselsnummer).stream()
            .map(this::tilEksternArbeidsforhold)
            .sorted(Comparator.comparing(EksternArbeidsforhold::arbeidsgiverNavn))
            .toList();
    }

    private EksternArbeidsforhold tilEksternArbeidsforhold(Arbeidsforhold a) {
        return new EksternArbeidsforhold(a.arbeidsforholdIdentifikator().arbeidsgiver(),
            tilArbeidsgiverTypeFrontend(a.arbeidsforholdIdentifikator()),
            a.ansettelsesPeriode().getFomDato(),
            Optional.of(a.ansettelsesPeriode().getTomDato()).filter(d -> d.isBefore(LocalDate.MAX)),
            gjeldendeStillingsprosent(a),
            arbeidsgiverNavn(a.arbeidsforholdIdentifikator()));
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

    private Stillingsprosent gjeldendeStillingsprosent(Arbeidsforhold arbeidsforhold) {
        return arbeidsforhold.arbeidsavtaler().stream()
            .filter(s -> erGjeldende(s.getLocalDateInterval()))
            .map(LocalDateSegment::getValue)
            .findFirst().orElse(null);
    }

    private boolean erGjeldende(LocalDateInterval interval) {
        var idag = LocalDate.now();
        return interval.contains(idag) || interval.getFomDato().isAfter(idag);
    }

    private String tilArbeidsgiverTypeFrontend(ArbeidsforholdIdentifikator a) {
        if (VirksomhetTjeneste.erOrganisasjonsNummer(a.arbeidsgiver())) {
            return "orgnr";
        } else if (erIdent(a.arbeidsgiver())) {
            return "fnr";
        } else {
            return null;
        }
    }

    private static boolean erIdent(String ident) {
        return ident != null && ident.matches("\\d{11}|\\d{13}");
    }
}
