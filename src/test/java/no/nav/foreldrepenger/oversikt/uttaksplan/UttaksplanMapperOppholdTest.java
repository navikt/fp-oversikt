package no.nav.foreldrepenger.oversikt.uttaksplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.domene.Prosent;
import no.nav.foreldrepenger.oversikt.domene.fp.BrukerRolle;
import no.nav.foreldrepenger.oversikt.domene.fp.FpSøknadsperiode;
import no.nav.foreldrepenger.oversikt.domene.fp.Konto;
import no.nav.foreldrepenger.oversikt.domene.fp.MorsAktivitet;
import no.nav.foreldrepenger.oversikt.domene.fp.OppholdÅrsak;
import no.nav.foreldrepenger.oversikt.domene.fp.Trekkdager;
import no.nav.foreldrepenger.oversikt.domene.fp.UtsettelseÅrsak;
import no.nav.foreldrepenger.oversikt.domene.fp.UttakAktivitet;
import no.nav.foreldrepenger.oversikt.domene.fp.Uttaksperiode;

class UttaksplanMapperOppholdTest {
    private static final LocalDate START = LocalDate.of(2026, 10, 5);

    @Test
    void skalUtelateOppholdOgFriutsettelseUtenMorsAktivitetFraVedtak() {
        var uttak = vedtaksperiode(START, null, null, null);
        var opphold = vedtaksperiode(START.plusWeeks(1), OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER, null, null);
        var friUtenAktivitet = vedtaksperiode(START.plusWeeks(2), null, UtsettelseÅrsak.FRI, null);
        var friMedAktivitet = vedtaksperiode(START.plusWeeks(3), null, UtsettelseÅrsak.FRI, MorsAktivitet.ARBEID);
        var annenUtsettelse = vedtaksperiode(START.plusWeeks(4), null, UtsettelseÅrsak.ARBEID, null);

        var perioder = UttaksplanMapper.mapVedtaksperioder(
            List.of(uttak, opphold, friUtenAktivitet, friMedAktivitet, annenUtsettelse), BrukerRolle.MOR);

        assertThat(perioder).extracting(UttaksplanTidslinje.Planperiode::fom)
            .containsExactly(uttak.fom(), friMedAktivitet.fom(), annenUtsettelse.fom());
    }

    @Test
    void skalUtelateOppholdOgFriutsettelseUtenMorsAktivitetFraSøknad() {
        var uttak = søknadsperiode(START, null, null, null);
        var opphold = søknadsperiode(START.plusWeeks(1), OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER, null, null);
        var friUtenAktivitet = søknadsperiode(START.plusWeeks(2), null, UtsettelseÅrsak.FRI, null);
        var friMedAktivitet = søknadsperiode(START.plusWeeks(3), null, UtsettelseÅrsak.FRI, MorsAktivitet.ARBEID);
        var annenUtsettelse = søknadsperiode(START.plusWeeks(4), null, UtsettelseÅrsak.ARBEID, null);

        var perioder = UttaksplanMapper.mapSøknadsperioder(
            List.of(uttak, opphold, friUtenAktivitet, friMedAktivitet, annenUtsettelse), BrukerRolle.MOR);

        assertThat(perioder).extracting(UttaksplanTidslinje.Planperiode::fom)
            .containsExactly(uttak.fom(), friMedAktivitet.fom(), annenUtsettelse.fom());
    }

    private static FpSøknadsperiode søknadsperiode(LocalDate fom, OppholdÅrsak opphold, UtsettelseÅrsak utsettelse, MorsAktivitet aktivitet) {
        return new FpSøknadsperiode(fom, fom.plusDays(4), utsettelse == null ? (opphold == null ? Konto.FELLESPERIODE : Konto.FEDREKVOTE) : null,
            utsettelse, opphold, null, null, null, false, aktivitet);
    }

    private static Uttaksperiode vedtaksperiode(LocalDate fom, OppholdÅrsak opphold, UtsettelseÅrsak utsettelse, MorsAktivitet morsAktivitet) {
        var aktivitet = new Uttaksperiode.UttaksperiodeAktivitet(new UttakAktivitet(UttakAktivitet.Type.FRILANS, null, null),
            opphold == null ? Konto.FELLESPERIODE : Konto.FEDREKVOTE, new Trekkdager(5), Prosent.ZERO);
        var resultat = new Uttaksperiode.Resultat(Uttaksperiode.Resultat.Type.INNVILGET,
            Uttaksperiode.Resultat.Årsak.ANNET, Set.of(aktivitet), false);
        return new Uttaksperiode(fom, fom.plusDays(4), utsettelse, opphold, null, Prosent.ZERO, false, morsAktivitet,
            utsettelse == null ? resultat : new Uttaksperiode.Resultat(Uttaksperiode.Resultat.Type.INNVILGET,
                Uttaksperiode.Resultat.Årsak.ANNET, Set.of(), false));
    }
}
