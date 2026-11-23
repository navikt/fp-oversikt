package no.nav.foreldrepenger.oversikt.uttaksplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.uttaksplan.UttaksplanTidslinje.Planperiode;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto;

class FødselspermisjonForslagFilterTest {

    // Torsdager, termin tre uker etter fødsel
    private static final LocalDate TERMIN = LocalDate.of(2026, 12, 10);
    private static final LocalDate FØDSEL = LocalDate.of(2026, 11, 19);
    private static final LocalDate TO_UKER_FRA_TERMIN = LocalDate.of(2026, 12, 23);

    @Test
    void farUtenSakFårIkkeForslagFraTerminNårBarnetErFødtFørTermin() {
        var forslag = samtidigFedrekvote(TERMIN, TO_UKER_FRA_TERMIN);

        assertThat(FødselspermisjonForslagFilter.fjernForslagFraTermin(List.of(forslag), født(FØDSEL, TERMIN))).isEmpty();
    }

    @Test
    void farUtenSakFårIkkeForslagFraTerminFørFødselen() {
        var forslag = samtidigFedrekvote(TERMIN, TO_UKER_FRA_TERMIN);

        assertThat(FødselspermisjonForslagFilter.fjernForslagFraTermin(List.of(forslag), termin(TERMIN))).isEmpty();
    }

    @Test
    void farUtenSakFårIkkeForslagFraTerminSomErFlyttetFraHelg() {
        var lørdag = LocalDate.of(2026, 12, 12);
        var forslag = samtidigFedrekvote(LocalDate.of(2026, 12, 14), LocalDate.of(2026, 12, 25));

        assertThat(FødselspermisjonForslagFilter.fjernForslagFraTermin(List.of(forslag), termin(lørdag))).isEmpty();
    }

    @Test
    void farUtenSakBeholderForslagSomIkkeStarterPåTermin() {
        var førTermin = samtidigFedrekvote(TERMIN.minusWeeks(2), TERMIN.minusDays(1));
        var fraFødsel = samtidigFedrekvote(FØDSEL, LocalDate.of(2026, 12, 2));
        var perioder = List.of(fraFødsel, førTermin);

        assertThat(FødselspermisjonForslagFilter.fjernForslagFraTermin(perioder, født(FØDSEL, TERMIN))).containsExactlyElementsOf(perioder);
    }

    @Test
    void farUtenSakBeholderFedrekvoteFraTerminUtenSamtidigUttak() {
        var fedrekvote = new Planperiode(TERMIN, TO_UKER_FRA_TERMIN, uttak(KontoType.FEDREKVOTE, null));

        assertThat(FødselspermisjonForslagFilter.fjernForslagFraTermin(List.of(fedrekvote), født(FØDSEL, TERMIN))).containsExactly(fedrekvote);
    }

    @Test
    void farUtenSakBeholderForslagVedAdopsjon() {
        var forslag = samtidigFedrekvote(TERMIN, TO_UKER_FRA_TERMIN);
        var adopsjon = new FamilieHendelse(null, null, 1, TERMIN);

        assertThat(FødselspermisjonForslagFilter.fjernForslagFraTermin(List.of(forslag), adopsjon)).containsExactly(forslag);
    }

    @Test
    void morSerForslagetNårHunHarSamtidigUttakISammeUker() {
        var forslag = samtidigFedrekvote(TERMIN, TO_UKER_FRA_TERMIN);
        var mors = morsSamtidigeUttak(TERMIN, TO_UKER_FRA_TERMIN);

        assertThat(FødselspermisjonForslagFilter.flyttForslagTilMorsSamtidigeUttak(List.of(forslag), List.of(mors), født(FØDSEL, TERMIN)))
            .containsExactly(forslag);
    }

    @Test
    void morSerForslagetFlyttetTilSittSamtidigeUttakVedFødsel() {
        var forslag = samtidigFedrekvote(TERMIN, TO_UKER_FRA_TERMIN);
        // Torsdag til onsdag, ti uttaksdager som forslaget
        var mors = morsSamtidigeUttak(FØDSEL, LocalDate.of(2026, 12, 2));

        assertThat(FødselspermisjonForslagFilter.flyttForslagTilMorsSamtidigeUttak(List.of(forslag), List.of(mors), født(FØDSEL, TERMIN)))
            .containsExactly(new Planperiode(FØDSEL, LocalDate.of(2026, 12, 2), forslag.uttak()));
    }

    @Test
    void morSerForslagetFlyttetTilSittFørsteSamtidigeUttak() {
        var forslag = samtidigFedrekvote(TERMIN, TO_UKER_FRA_TERMIN);
        var første = morsSamtidigeUttak(FØDSEL, LocalDate.of(2026, 12, 2));
        var senere = morsSamtidigeUttak(LocalDate.of(2027, 2, 1), LocalDate.of(2027, 2, 12));

        assertThat(FødselspermisjonForslagFilter.flyttForslagTilMorsSamtidigeUttak(List.of(forslag), List.of(senere, første), født(FØDSEL, TERMIN)))
            .containsExactly(new Planperiode(FØDSEL, LocalDate.of(2026, 12, 2), forslag.uttak()));
    }

    @Test
    void morSerIkkeForslagetNårHunIkkeHarSamtidigUttak() {
        var forslag = samtidigFedrekvote(TERMIN, TO_UKER_FRA_TERMIN);
        var senereSamtidigUttak = samtidigFedrekvote(LocalDate.of(2027, 2, 1), LocalDate.of(2027, 2, 12));
        var mors = new Planperiode(TERMIN, TO_UKER_FRA_TERMIN, morsUttak(null));

        assertThat(FødselspermisjonForslagFilter.flyttForslagTilMorsSamtidigeUttak(List.of(forslag, senereSamtidigUttak), List.of(mors),
            termin(TERMIN))).containsExactly(senereSamtidigUttak);
    }

    @Test
    void morSerIkkeForslagetNårHennesSamtidigeUttakHarAnnenLengde() {
        var forslag = samtidigFedrekvote(TERMIN, TO_UKER_FRA_TERMIN);
        var mors = morsSamtidigeUttak(FØDSEL, LocalDate.of(2026, 11, 25));

        assertThat(FødselspermisjonForslagFilter.flyttForslagTilMorsSamtidigeUttak(List.of(forslag), List.of(mors), født(FØDSEL, TERMIN)))
            .isEmpty();
    }

    @Test
    void morSerIkkeForslagetNårFlyttingGirOverlappMedFarsAndrePerioder() {
        var forslag = samtidigFedrekvote(TERMIN, TO_UKER_FRA_TERMIN);
        var farsAndrePeriode = new Planperiode(FØDSEL, FØDSEL.plusDays(1), uttak(KontoType.FEDREKVOTE, null));
        var mors = morsSamtidigeUttak(FØDSEL, LocalDate.of(2026, 12, 2));

        assertThat(FødselspermisjonForslagFilter.flyttForslagTilMorsSamtidigeUttak(List.of(farsAndrePeriode, forslag), List.of(mors),
            født(FØDSEL, TERMIN))).containsExactly(farsAndrePeriode);
    }

    private static Planperiode morsSamtidigeUttak(LocalDate fom, LocalDate tom) {
        return new Planperiode(fom, tom, morsUttak(new FellesUttaksplanDto.SamtidigUttak(BigDecimal.valueOf(100))));
    }

    private static FellesUttaksplanDto.UttakDto morsUttak(FellesUttaksplanDto.SamtidigUttak samtidigUttak) {
        return new FellesUttaksplanDto.UttakDto(FellesUttaksplanDto.Rolle.MOR, KontoType.MØDREKVOTE, null, null, null, null, samtidigUttak, false,
            null);
    }

    private static FamilieHendelse termin(LocalDate termindato) {
        return new FamilieHendelse(null, termindato, 1, null);
    }

    private static FamilieHendelse født(LocalDate fødselsdato, LocalDate termindato) {
        return new FamilieHendelse(fødselsdato, termindato, 1, null);
    }

    private static Planperiode samtidigFedrekvote(LocalDate fom, LocalDate tom) {
        return new Planperiode(fom, tom, uttak(KontoType.FEDREKVOTE, new FellesUttaksplanDto.SamtidigUttak(BigDecimal.valueOf(100))));
    }

    private static FellesUttaksplanDto.UttakDto uttak(KontoType konto, FellesUttaksplanDto.SamtidigUttak samtidigUttak) {
        return new FellesUttaksplanDto.UttakDto(FellesUttaksplanDto.Rolle.FAR_MEDMOR, konto, null, null, null, null, samtidigUttak, false, null);
    }
}
