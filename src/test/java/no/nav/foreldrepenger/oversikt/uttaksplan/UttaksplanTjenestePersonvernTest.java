package no.nav.foreldrepenger.oversikt.uttaksplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType;
import no.nav.foreldrepenger.kontrakter.fpoversikt.FellesUttaksplanDto;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.SøknadStatus;
import no.nav.foreldrepenger.oversikt.domene.fp.BrukerRolle;
import no.nav.foreldrepenger.oversikt.domene.fp.Dekningsgrad;
import no.nav.foreldrepenger.oversikt.domene.fp.ForeldrepengerSak;
import no.nav.foreldrepenger.oversikt.domene.fp.FpSøknad;
import no.nav.foreldrepenger.oversikt.domene.fp.FpSøknadsperiode;
import no.nav.foreldrepenger.oversikt.domene.fp.Konto;
import no.nav.foreldrepenger.oversikt.saker.AnnenPartSakTjeneste;
import no.nav.foreldrepenger.oversikt.saker.Saker;
import no.nav.foreldrepenger.oversikt.uttaksplan.UttaksplanTidslinje.Planperiode;

class UttaksplanTjenestePersonvernTest {

    @Test
    void skalIkkeHenteLagretAnnenPartNårAnnenPartErSkjermetEllerUkjent() {
        var søker = AktørId.dummy();
        var barn = AktørId.dummy();
        var saksnummer = Saksnummer.dummy();
        var termindato = LocalDate.of(2026, 10, 1);
        var søkersSak = mock(ForeldrepengerSak.class);
        when(søkersSak.saksnummer()).thenReturn(saksnummer);
        when(søkersSak.gjelderBarn(barn)).thenReturn(true);
        when(søkersSak.sisteSøknad()).thenReturn(Optional.of(
            new FpSøknad(SøknadStatus.MOTTATT, LocalDateTime.of(2026, 9, 1, 12, 0), Set.of(), Dekningsgrad.HUNDRE, false)));
        when(søkersSak.gjeldendeVedtak()).thenReturn(Optional.empty());
        when(søkersSak.familieHendelse()).thenReturn(new FamilieHendelse(null, termindato, 1, null));
        when(søkersSak.dekningsgrad()).thenReturn(Dekningsgrad.HUNDRE);
        when(søkersSak.brukerRolle()).thenReturn(BrukerRolle.MOR);
        var saker = mock(Saker.class);
        when(saker.hentSaker(søker)).thenReturn(List.of(søkersSak));
        var annenPartSakTjeneste = mock(AnnenPartSakTjeneste.class);
        var repository = mock(AnnenPartUttaksplanRepository.class);
        var lagretPeriode = new Planperiode(termindato, termindato.plusWeeks(1),
            new FellesUttaksplanDto.UttakDto(FellesUttaksplanDto.Rolle.FAR_MEDMOR, null, null, null, null, null, null, false, null));
        when(repository.hentFor(saksnummer)).thenReturn(Optional.of(new AnnenPartUttaksplan(LocalDateTime.now(), List.of(lagretPeriode))));
        var tjeneste = new UttaksplanTjeneste(saker, annenPartSakTjeneste, repository);

        var plan = tjeneste.hentFor(søker, null, barn, termindato).orElseThrow();

        assertThat(plan.perioder()).isEmpty();
        verifyNoInteractions(annenPartSakTjeneste, repository);
    }

    @Test
    void skalIkkeHenteLagretAnnenPartNårOppgittAktørIkkeMatcherSøkersSak() {
        var søker = AktørId.dummy();
        var forespurtAnnenPart = AktørId.dummy();
        var annenPartISak = AktørId.dummy();
        var barn = AktørId.dummy();
        var saksnummer = Saksnummer.dummy();
        var termindato = LocalDate.of(2026, 10, 1);
        var søkersSak = mock(ForeldrepengerSak.class);
        when(søkersSak.saksnummer()).thenReturn(saksnummer);
        when(søkersSak.gjelderBarn(barn)).thenReturn(true);
        when(søkersSak.sisteSøknad()).thenReturn(Optional.of(
            new FpSøknad(SøknadStatus.MOTTATT, LocalDateTime.of(2026, 9, 1, 12, 0), Set.of(), Dekningsgrad.HUNDRE, false)));
        when(søkersSak.gjeldendeVedtak()).thenReturn(Optional.empty());
        when(søkersSak.familieHendelse()).thenReturn(new FamilieHendelse(null, termindato, 1, null));
        when(søkersSak.dekningsgrad()).thenReturn(Dekningsgrad.HUNDRE);
        when(søkersSak.brukerRolle()).thenReturn(BrukerRolle.MOR);
        when(søkersSak.annenPartAktørId()).thenReturn(annenPartISak);
        var saker = mock(Saker.class);
        when(saker.hentSaker(søker)).thenReturn(List.of(søkersSak));
        var annenPartSakTjeneste = mock(AnnenPartSakTjeneste.class);
        when(annenPartSakTjeneste.annenPartGjeldendeSakOppgittSøker(søker, forespurtAnnenPart, barn, termindato)).thenReturn(Optional.empty());
        var repository = mock(AnnenPartUttaksplanRepository.class);
        var tjeneste = new UttaksplanTjeneste(saker, annenPartSakTjeneste, repository);

        var plan = tjeneste.hentFor(søker, forespurtAnnenPart, barn, termindato).orElseThrow();

        assertThat(plan.perioder()).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    void skalBrukePerioderAnnenPartHarForeslåttForSøkerNårSøkerIkkeHarEgneData() {
        var far = AktørId.dummy();
        var mor = AktørId.dummy();
        var barn = AktørId.dummy();
        var morsSaksnummer = Saksnummer.dummy();
        var termindato = LocalDate.of(2026, 10, 1);
        var morsPeriode = søknadsperiode(termindato, termindato.plusWeeks(2), Konto.MØDREKVOTE);
        var morsSak = sak(morsSaksnummer, mor, far, barn, termindato, BrukerRolle.MOR, List.of(morsPeriode));
        var foreslåttForFar = new Planperiode(termindato.plusWeeks(6), termindato.plusWeeks(8),
            new FellesUttaksplanDto.UttakDto(FellesUttaksplanDto.Rolle.FAR_MEDMOR, KontoType.FEDREKVOTE, null, null, null,
                null, null, false, null));
        var saker = mock(Saker.class);
        when(saker.hentSaker(far)).thenReturn(List.of());
        var annenPartSakTjeneste = mock(AnnenPartSakTjeneste.class);
        when(annenPartSakTjeneste.annenPartGjeldendeSakOppgittSøker(far, mor, barn, termindato)).thenReturn(Optional.of(morsSak));
        var repository = mock(AnnenPartUttaksplanRepository.class);
        when(repository.hentFor(morsSaksnummer)).thenReturn(
            Optional.of(new AnnenPartUttaksplan(LocalDateTime.of(2026, 9, 1, 12, 0), List.of(foreslåttForFar))));
        var tjeneste = new UttaksplanTjeneste(saker, annenPartSakTjeneste, repository);

        var plan = tjeneste.hentFor(far, mor, barn, termindato).orElseThrow();

        assertThat(plan.perioder()).anySatisfy(periode -> {
            assertThat(periode.fom()).isEqualTo(foreslåttForFar.fom());
            assertThat(periode.søker()).isEqualTo(foreslåttForFar.uttak());
            assertThat(periode.annenPart()).isNull();
        }).anySatisfy(periode -> {
            assertThat(periode.fom()).isEqualTo(morsPeriode.fom());
            assertThat(periode.søker()).isNull();
            assertThat(periode.annenPart()).isNotNull();
        });
    }

    @Test
    void skalAlltidBrukeSøkersEgnePerioderFremforAnnenPartsForslag() {
        var far = AktørId.dummy();
        var mor = AktørId.dummy();
        var barn = AktørId.dummy();
        var termindato = LocalDate.of(2026, 10, 1);
        var farsPeriode = søknadsperiode(termindato.plusWeeks(7), termindato.plusWeeks(9), Konto.FEDREKVOTE);
        var morsPeriode = søknadsperiode(termindato, termindato.plusWeeks(2), Konto.MØDREKVOTE);
        var farsSak = sak(Saksnummer.dummy(), far, mor, barn, termindato, BrukerRolle.FAR, List.of(farsPeriode));
        var morsSak = sak(Saksnummer.dummy(), mor, far, barn, termindato, BrukerRolle.MOR, List.of(morsPeriode));
        var saker = mock(Saker.class);
        when(saker.hentSaker(far)).thenReturn(List.of(farsSak));
        var annenPartSakTjeneste = mock(AnnenPartSakTjeneste.class);
        when(annenPartSakTjeneste.annenPartGjeldendeSakOppgittSøker(far, mor, barn, termindato)).thenReturn(Optional.of(morsSak));
        var repository = mock(AnnenPartUttaksplanRepository.class);
        var tjeneste = new UttaksplanTjeneste(saker, annenPartSakTjeneste, repository);

        var plan = tjeneste.hentFor(far, mor, barn, termindato).orElseThrow();

        assertThat(plan.perioder()).anySatisfy(periode -> {
            assertThat(periode.fom()).isEqualTo(farsPeriode.fom());
            assertThat(periode.søker()).isNotNull();
        });
        verifyNoInteractions(repository);
    }

    @Test
    void skalBehandleTomEgenSøknadsplanSomAutoritativ() {
        var far = AktørId.dummy();
        var mor = AktørId.dummy();
        var barn = AktørId.dummy();
        var termindato = LocalDate.of(2026, 10, 1);
        var farsSak = sak(Saksnummer.dummy(), far, mor, barn, termindato, BrukerRolle.FAR, List.of());
        var morsSak = sak(Saksnummer.dummy(), mor, far, barn, termindato, BrukerRolle.MOR,
            List.of(søknadsperiode(termindato, termindato.plusWeeks(2), Konto.MØDREKVOTE)));
        var saker = mock(Saker.class);
        when(saker.hentSaker(far)).thenReturn(List.of(farsSak));
        var annenPartSakTjeneste = mock(AnnenPartSakTjeneste.class);
        when(annenPartSakTjeneste.annenPartGjeldendeSakOppgittSøker(far, mor, barn, termindato)).thenReturn(Optional.of(morsSak));
        var repository = mock(AnnenPartUttaksplanRepository.class);
        var tjeneste = new UttaksplanTjeneste(saker, annenPartSakTjeneste, repository);

        var plan = tjeneste.hentFor(far, mor, barn, termindato).orElseThrow();

        assertThat(plan.perioder()).allSatisfy(periode -> {
            assertThat(periode.søker()).isNull();
            assertThat(periode.annenPart()).isNotNull();
        });
        verifyNoInteractions(repository);
    }

    private static ForeldrepengerSak sak(Saksnummer saksnummer,
                                         AktørId aktørId,
                                         AktørId annenPart,
                                         AktørId barn,
                                         LocalDate termindato,
                                         BrukerRolle rolle,
                                         List<FpSøknadsperiode> perioder) {
        var sak = mock(ForeldrepengerSak.class);
        when(sak.saksnummer()).thenReturn(saksnummer);
        when(sak.aktørId()).thenReturn(aktørId);
        when(sak.annenPartAktørId()).thenReturn(annenPart);
        when(sak.gjelderBarn(barn)).thenReturn(true);
        when(sak.gjeldendeVedtak()).thenReturn(Optional.empty());
        when(sak.sisteSøknad()).thenReturn(Optional.of(
            new FpSøknad(SøknadStatus.MOTTATT, LocalDateTime.of(2026, 9, 1, 12, 0), Set.copyOf(perioder), Dekningsgrad.HUNDRE, false)));
        when(sak.familieHendelse()).thenReturn(new FamilieHendelse(null, termindato, 1, null));
        when(sak.dekningsgrad()).thenReturn(Dekningsgrad.HUNDRE);
        when(sak.brukerRolle()).thenReturn(rolle);
        return sak;
    }

    private static FpSøknadsperiode søknadsperiode(LocalDate fom, LocalDate tom, Konto konto) {
        return new FpSøknadsperiode(fom, tom, konto, null, null, null, null, null, false, null);
    }
}
