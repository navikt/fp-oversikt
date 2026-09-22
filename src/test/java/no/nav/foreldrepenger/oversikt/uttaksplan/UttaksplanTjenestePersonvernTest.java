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

import no.nav.foreldrepenger.kontrakter.fpoversikt.FellesUttaksplanDto;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.SøknadStatus;
import no.nav.foreldrepenger.oversikt.domene.fp.BrukerRolle;
import no.nav.foreldrepenger.oversikt.domene.fp.Dekningsgrad;
import no.nav.foreldrepenger.oversikt.domene.fp.ForeldrepengerSak;
import no.nav.foreldrepenger.oversikt.domene.fp.FpSøknad;
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
}
