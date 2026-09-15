package no.nav.foreldrepenger.oversikt.uttaksplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.kontrakter.felles.kodeverk.KontoType;
import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;
import no.nav.foreldrepenger.oversikt.domene.Prosent;
import no.nav.foreldrepenger.oversikt.domene.fp.BrukerRolle;
import no.nav.foreldrepenger.oversikt.domene.fp.FpSøknadsperiode;
import no.nav.foreldrepenger.oversikt.domene.fp.Konto;
import no.nav.foreldrepenger.oversikt.domene.fp.Trekkdager;
import no.nav.foreldrepenger.oversikt.domene.fp.UttakAktivitet;
import no.nav.foreldrepenger.oversikt.domene.fp.UttakPeriodeAnnenpartEøs;
import no.nav.foreldrepenger.oversikt.domene.fp.Uttaksperiode;
import no.nav.foreldrepenger.oversikt.uttaksplan.UttaksplanTidslinje.Planperiode;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

class UttaksplanTidslinjeTest {

    // 2024-01-01 er en mandag.
    private static final LocalDate MANDAG = LocalDate.of(2024, 1, 1);

    @Test
    void skalGiTomPlanNårIngenAvParteneHarUttak() {
        var plan = UttaksplanTidslinje.normaliser(List.of(), List.of(), List.of());

        assertThat(perioder(plan)).isEmpty();
    }

    @Test
    void skalMappeSøkerensPerioderMedSøkerensRolle() {
        var søker = uttak(BrukerRolle.MOR, periode(MANDAG, MANDAG.plusDays(4), Konto.MØDREKVOTE));

        var plan = UttaksplanTidslinje.normaliser(søker, List.of(), List.of());

        assertThat(perioder(plan)).hasSize(1);
        var periode = perioder(plan).getFirst();
        assertThat(periode.fom()).isEqualTo(MANDAG);
        assertThat(periode.tom()).isEqualTo(MANDAG.plusDays(4));
        assertThat(periode.søker()).isNotNull();
        assertThat(periode.søker().forelder()).isEqualTo(FellesUttaksplanDto.Rolle.MOR);
        assertThat(periode.annenPart()).isNull();
        assertThat(periode.annenPartEøs()).isNull();
    }

    @Test
    void skalJustereVedtaksperiodeFraHelgTilVirkedager() {
        var lørdag = MANDAG.plusDays(5);
        var søndagUkenEtter = MANDAG.plusDays(13);

        var planperioder = UttaksplanMapper.mapVedtaksperioder(List.of(periode(lørdag, søndagUkenEtter, Konto.MØDREKVOTE)), BrukerRolle.MOR);

        assertThat(planperioder).singleElement().satisfies(p -> {
            assertThat(p.fom()).isEqualTo(MANDAG.plusWeeks(1));
            assertThat(p.tom()).isEqualTo(MANDAG.plusDays(11));
        });
    }

    @Test
    void skalJustereSøknadsperiodeFraHelgTilVirkedager() {
        var lørdag = MANDAG.plusDays(5);
        var søndagUkenEtter = MANDAG.plusDays(13);
        var søknadsperiode = new FpSøknadsperiode(lørdag, søndagUkenEtter, Konto.MØDREKVOTE, null, null, null, null, null, false, null);

        var planperioder = UttaksplanMapper.mapSøknadsperioder(List.of(søknadsperiode), BrukerRolle.MOR);

        assertThat(planperioder).singleElement().satisfies(p -> {
            assertThat(p.fom()).isEqualTo(MANDAG.plusWeeks(1));
            assertThat(p.tom()).isEqualTo(MANDAG.plusDays(11));
        });
    }

    @Test
    void skalFjerneNorskPeriodeSomBareLiggerIHelg() {
        var lørdag = MANDAG.plusDays(5);
        var søndag = MANDAG.plusDays(6);

        var vedtaksperioder = UttaksplanMapper.mapVedtaksperioder(List.of(periode(lørdag, søndag, Konto.MØDREKVOTE)), BrukerRolle.MOR);
        var søknadsperiode = new FpSøknadsperiode(lørdag, søndag, Konto.MØDREKVOTE, null, null, null, null, null, false, null);
        var søknadsperioder = UttaksplanMapper.mapSøknadsperioder(List.of(søknadsperiode), BrukerRolle.MOR);

        assertThat(vedtaksperioder).isEmpty();
        assertThat(søknadsperioder).isEmpty();
    }

    @Test
    void skalMappeFarOgMedmorTilSammeRolleIDto() {
        var far = uttak(BrukerRolle.FAR, periode(MANDAG, MANDAG.plusDays(4), Konto.FEDREKVOTE));
        var medmor = uttak(BrukerRolle.MEDMOR, periode(MANDAG, MANDAG.plusDays(4), Konto.FEDREKVOTE));

        var farsPlan = UttaksplanTidslinje.normaliser(far, List.of(), List.of());
        var medmorsPlan = UttaksplanTidslinje.normaliser(medmor, List.of(), List.of());

        assertThat(perioder(farsPlan).getFirst().søker().forelder()).isEqualTo(FellesUttaksplanDto.Rolle.FAR_MEDMOR);
        assertThat(perioder(medmorsPlan).getFirst().søker().forelder()).isEqualTo(FellesUttaksplanDto.Rolle.FAR_MEDMOR);
    }

    @Test
    void skalMappeAnnenPartsPerioderNårSøkerIkkeHarUttak() {
        var annenPart = uttak(BrukerRolle.FAR, periode(MANDAG, MANDAG.plusDays(4), Konto.FEDREKVOTE));

        var plan = UttaksplanTidslinje.normaliser(List.of(), annenPart, List.of());

        assertThat(perioder(plan)).hasSize(1);
        assertThat(perioder(plan).getFirst().søker()).isNull();
        assertThat(perioder(plan).getFirst().annenPart()).isNotNull();
        assertThat(perioder(plan).getFirst().annenPart().forelder()).isEqualTo(FellesUttaksplanDto.Rolle.FAR_MEDMOR);
    }

    @Test
    void skalSortereSegmenteneKronologisk() {
        var søker = uttak(BrukerRolle.MOR, periode(MANDAG.plusWeeks(4), MANDAG.plusWeeks(5), Konto.MØDREKVOTE),
            periode(MANDAG, MANDAG.plusDays(4), Konto.MØDREKVOTE));

        var plan = UttaksplanTidslinje.normaliser(søker, List.of(), List.of());

        assertThat(perioder(plan)).isSortedAccordingTo((a, b) -> a.fom().compareTo(b.fom()));
        assertThat(perioder(plan).getFirst().fom()).isEqualTo(MANDAG);
    }

    @Test
    void skalIkkeProdusereOverlappendeSegmenter() {
        var søker = uttak(BrukerRolle.MOR, periode(MANDAG, MANDAG.plusWeeks(4), Konto.MØDREKVOTE));
        var annenPart = uttak(BrukerRolle.FAR, periode(MANDAG.plusWeeks(2), MANDAG.plusWeeks(6), Konto.FEDREKVOTE));

        var plan = UttaksplanTidslinje.normaliser(søker, annenPart, List.of());

        var perioder = perioder(plan);
        for (var i = 1; i < perioder.size(); i++) {
            assertThat(perioder.get(i).fom()).isAfter(perioder.get(i - 1).tom());
        }
    }

    @Test
    void skalBevareBeggeParterVedSamtidigUttak() {
        var søker = uttak(BrukerRolle.MOR, periode(MANDAG, MANDAG.plusWeeks(4).minusDays(1), Konto.FELLESPERIODE));
        var annenPart = uttak(BrukerRolle.FAR, periode(MANDAG.plusWeeks(2), MANDAG.plusWeeks(6).minusDays(1), Konto.FEDREKVOTE));

        var plan = UttaksplanTidslinje.normaliser(søker, annenPart, List.of());

        var overlapp = perioder(plan).stream().filter(p -> p.søker() != null && p.annenPart() != null).toList();
        assertThat(overlapp).hasSize(1);
        assertThat(overlapp.getFirst().fom()).isEqualTo(MANDAG.plusWeeks(2));
        assertThat(overlapp.getFirst().tom()).isEqualTo(MANDAG.plusWeeks(4).minusDays(3));

        var kunSøker = perioder(plan).stream().filter(p -> p.søker() != null && p.annenPart() == null).toList();
        assertThat(kunSøker).hasSize(1);
        assertThat(kunSøker.getFirst().fom()).isEqualTo(MANDAG);

        var kunAnnenPart = perioder(plan).stream().filter(p -> p.søker() == null && p.annenPart() != null).toList();
        assertThat(kunAnnenPart).hasSize(1);
        assertThat(kunAnnenPart.getFirst().tom()).isEqualTo(MANDAG.plusWeeks(6).minusDays(3));
    }

    @Test
    void skalSetteHundreProsentPåUgradertPartNårBareMotpartenHarSamtidigUttak() {
        var søker = uttak(BrukerRolle.MOR, periodeMedSamtidigUttak(MANDAG, MANDAG.plusDays(4), Konto.MØDREKVOTE, 100));
        var annenPart = uttak(BrukerRolle.FAR, periode(MANDAG, MANDAG.plusDays(4), Konto.FEDREKVOTE));

        var plan = UttaksplanTidslinje.normaliser(søker, annenPart, List.of());

        var overlapp = perioder(plan).getFirst();
        assertThat(overlapp.søker().samtidigUttak().value()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(overlapp.annenPart().samtidigUttak().value()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void skalGiSøkerPrioritetVedOverlappMedEøs() {
        var søker = uttak(BrukerRolle.MOR, periode(MANDAG, MANDAG.plusDays(1), Konto.MØDREKVOTE));
        var eøs = eøsPeriode(MANDAG, MANDAG.plusDays(4), Konto.FEDREKVOTE, 5);

        var plan = UttaksplanTidslinje.normaliser(søker, List.of(), List.of(eøs));

        var medSøker = perioder(plan).stream().filter(p -> p.søker() != null).toList();
        assertThat(medSøker).hasSize(1);
        assertThat(medSøker.getFirst().fom()).isEqualTo(MANDAG);
        assertThat(medSøker.getFirst().tom()).isEqualTo(MANDAG.plusDays(1));
        assertThat(medSøker.getFirst().annenPartEøs()).isNull();

        var medEøs = perioder(plan).stream().filter(p -> p.annenPartEøs() != null).toList();
        assertThat(medEøs).hasSize(1);
        assertThat(medEøs.getFirst().fom()).isEqualTo(MANDAG.plusDays(2));
    }

    /**
     * Annen part har enten norsk eller EØS-uttak, aldri begge deler. Finnes det EØS-perioder,
     * bygges planen uten annen parts norske uttak i det hele tatt, ikke bare i de overlappende
     * segmentene.
     */
    @Test
    void skalIkkeTaMedAnnenPartsNorskeUttakNårAnnenPartHarEøsPerioder() {
        var søker = uttak(BrukerRolle.MOR, periode(MANDAG, MANDAG.plusWeeks(2).minusDays(1), Konto.MØDREKVOTE));
        var annenPart = uttak(BrukerRolle.FAR, periode(MANDAG, MANDAG.plusWeeks(4).minusDays(1), Konto.FEDREKVOTE));
        var eøs = eøsPeriode(MANDAG, MANDAG.plusWeeks(4).minusDays(1), Konto.FEDREKVOTE, 20);

        var plan = UttaksplanTidslinje.normaliser(søker, annenPart, List.of(eøs));

        assertThat(perioder(plan)).noneMatch(p -> p.annenPart() != null);
        assertThat(perioder(plan)).anyMatch(p -> p.annenPartEøs() != null);
    }

    @Test
    void skalKopiereEøsTrekkdageneUendretNårSøkerensUttakSplitterPerioden() {
        // Summen blir for høy, jf. TODO i tilEøsTimeline.
        var søker = uttak(BrukerRolle.MOR, periode(MANDAG, MANDAG.plusDays(1), Konto.MØDREKVOTE));
        var eøs = eøsPeriode(MANDAG, MANDAG.plusDays(4), Konto.FEDREKVOTE, 5);

        var plan = UttaksplanTidslinje.normaliser(søker, List.of(), List.of(eøs));

        var medEøs = perioder(plan).stream().filter(p -> p.annenPartEøs() != null).toList();
        assertThat(medEøs).hasSize(1);
        assertThat(medEøs.getFirst().annenPartEøs().trekkdager().verdi()).isEqualByComparingTo(BigDecimal.valueOf(5));
    }

    @Test
    void skalBeholdeEøsPeriodenUrørtNårIngenOverlapper() {
        var eøs = eøsPeriode(MANDAG, MANDAG.plusDays(4), Konto.FEDREKVOTE, 5);

        var plan = UttaksplanTidslinje.normaliser(List.of(), List.of(), List.of(eøs));

        assertThat(perioder(plan)).hasSize(1);
        var periode = perioder(plan).getFirst();
        assertThat(periode.fom()).isEqualTo(MANDAG);
        assertThat(periode.tom()).isEqualTo(MANDAG.plusDays(4));
        assertThat(periode.annenPartEøs().trekkdager().verdi()).isEqualByComparingTo(BigDecimal.valueOf(5));
    }

    @Test
    void skalKomprimereLikeEøsSegmenterSomHengerSammenOverHelg() {
        var fredag = MANDAG.plusDays(4);
        var nesteMandag = MANDAG.plusWeeks(1);
        var eøs = List.of(eøsPeriode(MANDAG, fredag, Konto.FEDREKVOTE, 5),
            eøsPeriode(nesteMandag, nesteMandag.plusDays(4), Konto.FEDREKVOTE, 3));

        var plan = UttaksplanTidslinje.normaliser(List.of(), List.of(), eøs);

        assertThat(perioder(plan)).hasSize(1);
        assertThat(perioder(plan).getFirst().fom()).isEqualTo(MANDAG);
        assertThat(perioder(plan).getFirst().tom()).isEqualTo(nesteMandag.plusDays(4));
        assertThat(perioder(plan).getFirst().annenPartEøs().trekkdager().verdi()).isEqualByComparingTo(BigDecimal.valueOf(8));
    }

    @Test
    void skalIkkeKomprimereEøsSegmenterMedUlikKontoType() {
        var fredag = MANDAG.plusDays(4);
        var nesteMandag = MANDAG.plusWeeks(1);
        var eøs = List.of(eøsPeriode(MANDAG, fredag, Konto.FEDREKVOTE, 5),
            eøsPeriode(nesteMandag, nesteMandag.plusDays(4), Konto.FELLESPERIODE, 3));

        var plan = UttaksplanTidslinje.normaliser(List.of(), List.of(), eøs);

        assertThat(perioder(plan)).hasSize(2);
    }

    @Test
    void skalKomprimereLikeSegmenterSomHengerSammenOverHelg() {
        // Fredag og påfølgende mandag hører til samme uttak og skal ende som ett segment.
        var søker = uttak(BrukerRolle.MOR, periode(MANDAG, MANDAG.plusDays(4), Konto.MØDREKVOTE),
            periode(MANDAG.plusDays(7), MANDAG.plusDays(11), Konto.MØDREKVOTE));

        var plan = UttaksplanTidslinje.normaliser(søker, List.of(), List.of());

        assertThat(perioder(plan)).hasSize(1);
        assertThat(perioder(plan).getFirst().fom()).isEqualTo(MANDAG);
        assertThat(perioder(plan).getFirst().tom()).isEqualTo(MANDAG.plusDays(11));
    }

    @Test
    void skalIkkeKomprimereSegmenterMedUlikKontoType() {
        var søker = uttak(BrukerRolle.MOR, periode(MANDAG, MANDAG.plusDays(4), Konto.MØDREKVOTE),
            periode(MANDAG.plusDays(7), MANDAG.plusDays(11), Konto.FELLESPERIODE));

        var plan = UttaksplanTidslinje.normaliser(søker, List.of(), List.of());

        assertThat(perioder(plan)).hasSize(2);
    }

    @Test
    void farSøkerFørstegangUtenEgenSakOgSerMorsVedtak() {
        // Det vanligste tilfellet: far søker for første gang og har ingen egen sak.
        var førFødsel = periode(MANDAG.minusWeeks(3), MANDAG.minusDays(3), Konto.FORELDREPENGER_FØR_FØDSEL);
        var mødrekvote = periode(MANDAG, MANDAG.plusWeeks(15).minusDays(3), Konto.MØDREKVOTE);
        var fellesperiode = periode(MANDAG.plusWeeks(15), MANDAG.plusWeeks(31).minusDays(3), Konto.FELLESPERIODE);
        var mor = uttak(BrukerRolle.MOR, førFødsel, mødrekvote, fellesperiode);

        var plan = UttaksplanTidslinje.normaliser(List.of(), mor, List.of());

        assertThat(perioder(plan)).hasSize(3);
        assertThat(perioder(plan)).allSatisfy(periode -> {
            assertThat(periode.søker()).isNull();
            assertThat(periode.annenPartEøs()).isNull();
            assertThat(periode.annenPart()).isNotNull();
            assertThat(periode.annenPart().forelder()).isEqualTo(FellesUttaksplanDto.Rolle.MOR);
        });
        assertThat(perioder(plan)).extracting(periode -> periode.annenPart().kontoType())
            .containsExactly(KontoType.FORELDREPENGER_FØR_FØDSEL, KontoType.MØDREKVOTE, KontoType.FELLESPERIODE);
        assertThat(perioder(plan).getFirst().fom()).isEqualTo(MANDAG.minusWeeks(3));
        assertThat(perioder(plan).getLast().tom()).isEqualTo(MANDAG.plusWeeks(31).minusDays(3));
    }

    @Test
    void tomtUttakGirTidslinjeUtenSegmenter() {
        assertThat(UttaksplanTidslinje.tilTimeline(List.of()).isEmpty()).isTrue();
    }

    @Test
    void sakUtenPerioderGirTidslinjeUtenSegmenter() {
        assertThat(UttaksplanTidslinje.tilTimeline(List.of()).isEmpty()).isTrue();
    }

    @Test
    void skalLageEtSegmentPerPeriode() {
        var uttak = uttak(BrukerRolle.MOR, periode(MANDAG, MANDAG.plusDays(4), Konto.MØDREKVOTE),
            periode(MANDAG.plusWeeks(4), MANDAG.plusWeeks(5), Konto.FELLESPERIODE));

        var timeline = UttaksplanTidslinje.tilTimeline(uttak);

        assertThat(timeline.size()).isEqualTo(2);
        assertThat(timeline.getMinLocalDate()).isEqualTo(MANDAG);
        assertThat(timeline.getMaxLocalDate()).isEqualTo(MANDAG.plusWeeks(5));
    }

    @Test
    void skalKasteVedOverlappendePerioderHosSammePart() {
        var uttak = uttak(BrukerRolle.MOR, periode(MANDAG, MANDAG.plusWeeks(4), Konto.MØDREKVOTE),
            periode(MANDAG.plusWeeks(2), MANDAG.plusWeeks(6), Konto.FELLESPERIODE));

        assertThatThrownBy(() -> UttaksplanTidslinje.tilTimeline(uttak)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void skalVelgeKontoenMedFlestTrekkdagerSlikSomUttaksperiodeGjør() {
        var utenTrekk = aktivitet(Konto.FORELDREPENGER_FØR_FØDSEL, 0);
        var færrest = aktivitet(Konto.FELLESPERIODE, 3);
        var flest = aktivitet(Konto.MØDREKVOTE, 10);
        var uttak = uttak(BrukerRolle.MOR, periode(MANDAG, MANDAG.plusDays(4), utenTrekk, færrest, flest));

        var timeline = UttaksplanTidslinje.tilTimeline(uttak);

        assertThat(timeline.stream().toList().getFirst().getValue().kontoType()).isEqualTo(KontoType.MØDREKVOTE);
    }

    @Test
    void skalIkkeHaKontoTypeNårPeriodenManglerAktiviteter() {
        var uttak = uttak(BrukerRolle.MOR, periode(MANDAG, MANDAG.plusDays(4)));

        var timeline = UttaksplanTidslinje.tilTimeline(uttak);

        assertThat(timeline.stream().toList().getFirst().getValue().kontoType()).isNull();
    }

    @Test
    void skalMappeEøsPeriodeTilSegmentMedKontoOgTrekkdager() {
        var eøs = eøsPeriode(MANDAG, MANDAG.plusDays(4), Konto.FEDREKVOTE, 5);

        var timeline = UttaksplanTidslinje.tilEøsTimeline(List.of(eøs));

        var segment = timeline.stream().toList().getFirst();
        assertThat(segment.getFom()).isEqualTo(MANDAG);
        assertThat(segment.getTom()).isEqualTo(MANDAG.plusDays(4));
        assertThat(segment.getValue().kontoType()).isEqualTo(KontoType.FEDREKVOTE);
        assertThat(segment.getValue().trekkdager().verdi()).isEqualByComparingTo(BigDecimal.valueOf(5));
    }

    @Test
    void skalKopiereTrekkdageneTilBeggeDelerNårEøsPeriodenSplittes() {
        var eøs = eøsPeriode(MANDAG, MANDAG.plusDays(4), Konto.FEDREKVOTE, 5);
        var splittet = UttaksplanTidslinje.tilEøsTimeline(List.of(eøs)).splitAtRegular(MANDAG, MANDAG.plusDays(4), Period.ofDays(2));

        // Dokumenterer den kjente feilen, jf. TODO i tilEøsTimeline.
        assertThat(splittet.stream()).hasSizeGreaterThan(1);
        assertThat(splittet.stream()).allSatisfy(
            segment -> assertThat(segment.getValue().trekkdager().verdi()).isEqualByComparingTo(BigDecimal.valueOf(5)));
    }

    @Test
    void skalGiTomTimelineNårDetIkkeFinnesEøsPerioder() {
        assertThat(UttaksplanTidslinje.tilEøsTimeline(List.of()).isEmpty()).isTrue();
    }

    @Test
    void skalFjerneAvslåttPeriodeUtenTrekkdager() {
        var avslått = avslåttPeriode(MANDAG, MANDAG.plusDays(4), Konto.FELLESPERIODE, 0);
        var innvilget = periode(MANDAG.plusWeeks(1), MANDAG.plusWeeks(1).plusDays(4), Konto.MØDREKVOTE);

        var timeline = UttaksplanTidslinje.tilTimeline(uttak(BrukerRolle.MOR, avslått, innvilget));

        assertThat(timeline.stream()).hasSize(1);
        assertThat(timeline.stream().toList().getFirst().getFom()).isEqualTo(MANDAG.plusWeeks(1));
    }

    @Test
    void skalBeholdeAvslåttPeriodeSomTrekkerDager() {
        var avslått = avslåttPeriode(MANDAG, MANDAG.plusDays(4), Konto.FELLESPERIODE, 5);

        var timeline = UttaksplanTidslinje.tilTimeline(uttak(BrukerRolle.MOR, avslått));

        assertThat(timeline.stream()).hasSize(1);
        assertThat(timeline.stream().toList().getFirst().getValue().resultat().innvilget()).isFalse();
    }

    @Test
    void skalBeholdeInnvilgetPeriodeSelvOmDenIkkeTrekkerDager() {
        var uttak = uttak(BrukerRolle.MOR, periode(MANDAG, MANDAG.plusDays(4), aktivitet(Konto.MØDREKVOTE, 0)));

        var timeline = UttaksplanTidslinje.tilTimeline(uttak);

        assertThat(timeline.stream()).hasSize(1);
    }

    @Test
    void skalFjerneAvslåttePerioderUtenTrekkdagerOgsåForAnnenPart() {
        var avslått = avslåttPeriode(MANDAG, MANDAG.plusDays(4), Konto.FEDREKVOTE, 0);

        var timeline = UttaksplanTidslinje.tilTimeline(uttak(BrukerRolle.FAR, avslått));

        assertThat(timeline.isEmpty()).isTrue();
    }

    @Test
    void skalBeholdeAvslåttPeriodeUtenAktiviteter() {
        var resultat = new Uttaksperiode.Resultat(Uttaksperiode.Resultat.Type.AVSLÅTT, Uttaksperiode.Resultat.Årsak.ANNET, Set.of(), false);
        var utenAktiviteter = new Uttaksperiode(MANDAG, MANDAG.plusDays(4), null, null, null, Prosent.ZERO, false, null, resultat);

        var timeline = UttaksplanTidslinje.tilTimeline(uttak(BrukerRolle.MOR, utenAktiviteter));

        assertThat(timeline.stream()).hasSize(1);
    }

    private static Uttaksperiode avslåttPeriode(LocalDate fom, LocalDate tom, Konto konto, int trekkdager) {
        var resultat = new Uttaksperiode.Resultat(Uttaksperiode.Resultat.Type.AVSLÅTT, Uttaksperiode.Resultat.Årsak.ANNET,
            Set.of(aktivitet(konto, trekkdager)), false);
        return new Uttaksperiode(fom, tom, null, null, null, Prosent.ZERO, false, null, resultat);
    }

    private static List<FellesUttaksplanDto.UttakPeriodeDto> perioder(LocalDateTimeline<FellesUttaksplanDto.UttakPeriodeDto> tidslinje) {
        return tidslinje.stream().map(LocalDateSegment::getValue).toList();
    }

    private static List<Planperiode> uttak(BrukerRolle rolle, Uttaksperiode... perioder) {
        return UttaksplanMapper.mapVedtaksperioder(List.of(perioder), rolle);
    }

    private static Uttaksperiode periode(LocalDate fom, LocalDate tom, Konto konto) {
        return periode(fom, tom, aktivitet(konto, 10));
    }

    private static Uttaksperiode periode(LocalDate fom, LocalDate tom, Uttaksperiode.UttaksperiodeAktivitet... aktiviteter) {
        var resultat = new Uttaksperiode.Resultat(Uttaksperiode.Resultat.Type.INNVILGET, Uttaksperiode.Resultat.Årsak.ANNET,
            Set.of(aktiviteter), false);
        return new Uttaksperiode(fom, tom, null, null, null, Prosent.ZERO, false, null, resultat);
    }

    private static Uttaksperiode periodeMedSamtidigUttak(LocalDate fom, LocalDate tom, Konto konto, int samtidigUttak) {
        var resultat = new Uttaksperiode.Resultat(Uttaksperiode.Resultat.Type.INNVILGET, Uttaksperiode.Resultat.Årsak.ANNET,
            Set.of(aktivitet(konto, 10)), false);
        return new Uttaksperiode(fom, tom, null, null, null, new Prosent(samtidigUttak), false, null, resultat);
    }

    private static Uttaksperiode.UttaksperiodeAktivitet aktivitet(Konto konto, int trekkdager) {
        return new Uttaksperiode.UttaksperiodeAktivitet(new UttakAktivitet(UttakAktivitet.Type.ORDINÆRT_ARBEID, Arbeidsgiver.dummy(), null), konto,
            new Trekkdager(trekkdager), Prosent.ZERO);
    }

    private static UttakPeriodeAnnenpartEøs eøsPeriode(LocalDate fom, LocalDate tom, Konto konto, int trekkdager) {
        return new UttakPeriodeAnnenpartEøs(fom, tom, konto, BigDecimal.valueOf(trekkdager));
    }
}
