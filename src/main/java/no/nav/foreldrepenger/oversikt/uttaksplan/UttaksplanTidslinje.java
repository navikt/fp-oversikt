package no.nav.foreldrepenger.oversikt.uttaksplan;

import static no.nav.foreldrepenger.kontrakter.fpoversikt.FellesUttaksplanDto.EøsUttakDto;
import static no.nav.foreldrepenger.kontrakter.fpoversikt.FellesUttaksplanDto.UttakDto;
import static no.nav.foreldrepenger.kontrakter.fpoversikt.FellesUttaksplanDto.UttakPeriodeDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.oversikt.domene.fp.UttakPeriodeAnnenpartEøs;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

public final class UttaksplanTidslinje {

    private UttaksplanTidslinje() {
    }

    public record Planperiode(LocalDate fom, LocalDate tom, UttakDto uttak) {
    }

    /**
     * Sammenstiller partenes uttak til en sortert, overlappsfri tidslinje sett fra søkerens side.
     */
    public static LocalDateTimeline<UttakPeriodeDto> normaliser(List<Planperiode> søker,
                                                                List<Planperiode> annenPart,
                                                                List<UttakPeriodeAnnenpartEøs> annenPartsEøsPerioder) {

        var søkerTimeline = tilTimeline(søker);
        if (!annenPartsEøsPerioder.isEmpty()) {
            return normaliserAnnenPartEøs(annenPartsEøsPerioder, søkerTimeline);
        }
        return normaliserAnnenPartNorsk(annenPart, søkerTimeline);
    }

    private static LocalDateTimeline<UttakPeriodeDto> normaliserAnnenPartNorsk(List<Planperiode> annenPart,
                                                                               LocalDateTimeline<UttakDto> søkerTimeline) {
        var annenPartTimeline = tilTimeline(annenPart);
        return søkerTimeline.union(annenPartTimeline, (intervall, søkerSegment, annenPartSegment) -> {
                var søkerUttak = søkerSegment == null ? null : søkerSegment.getValue();
                var annenPartUttak = annenPartSegment == null ? null : annenPartSegment.getValue();
                var normalisert = SamtidigUttakNormalisering.normaliser(søkerUttak, annenPartUttak);
                return nyttSegment(intervall, normalisert.søker(), normalisert.annenPart(), null);
            })
            .compress(LocalDateInterval::abutsWorkdays, UttaksplanTidslinje::harLikUttaksinformasjon,
                (intervall, venstre, _) -> nyttSegment(intervall, venstre.getValue().søker(), venstre.getValue().annenPart(),
                    venstre.getValue().annenPartEøs()));
    }

    private static LocalDateTimeline<UttakPeriodeDto> normaliserAnnenPartEøs(List<UttakPeriodeAnnenpartEøs> annenPartsEøsPerioder,
                                                                             LocalDateTimeline<UttakDto> søkerTimeline) {
        var eøsTimeline = tilEøsTimeline(annenPartsEøsPerioder);
        return søkerTimeline.union(eøsTimeline, (intervall, søkerSegment, eøsSegment) -> {
            var søkerUttak = søkerSegment == null ? null : søkerSegment.getValue();
            var eøsUttak = søkerSegment == null && eøsSegment != null ? eøsSegment.getValue() : null;
            return nyttSegment(intervall, søkerUttak, null, eøsUttak);
        }).compress(LocalDateInterval::abutsWorkdays, UttaksplanTidslinje::harLikEøsUttaksinformasjon, UttaksplanTidslinje::slåSammenEøsSegmenter);
    }

    private static LocalDateSegment<UttakPeriodeDto> nyttSegment(LocalDateInterval intervall,
                                                                 UttakDto søker,
                                                                 UttakDto annenPart,
                                                                 EøsUttakDto annenPartEøs) {
        return new LocalDateSegment<>(intervall, new UttakPeriodeDto(intervall.getFomDato(), intervall.getTomDato(), søker, annenPart, annenPartEøs));
    }

    private static boolean harLikUttaksinformasjon(UttakPeriodeDto venstre, UttakPeriodeDto høyre) {
        return Objects.equals(venstre.søker(), høyre.søker()) && Objects.equals(venstre.annenPart(), høyre.annenPart()) && Objects.equals(
            venstre.annenPartEøs(), høyre.annenPartEøs());
    }

    private static boolean harLikEøsUttaksinformasjon(UttakPeriodeDto venstre, UttakPeriodeDto høyre) {
        if (!Objects.equals(venstre.søker(), høyre.søker()) || !Objects.equals(venstre.annenPart(), høyre.annenPart())) {
            return false;
        }
        if (venstre.annenPartEøs() == null || høyre.annenPartEøs() == null) {
            return venstre.annenPartEøs() == høyre.annenPartEøs();
        }
        return venstre.annenPartEøs().kontoType() == høyre.annenPartEøs().kontoType();
    }

    private static LocalDateSegment<UttakPeriodeDto> slåSammenEøsSegmenter(LocalDateInterval intervall,
                                                                           LocalDateSegment<UttakPeriodeDto> venstre,
                                                                           LocalDateSegment<UttakPeriodeDto> høyre) {
        var venstreVerdi = venstre.getValue();
        if (venstreVerdi.annenPartEøs() == null) {
            return nyttSegment(intervall, venstreVerdi.søker(), venstreVerdi.annenPart(), null);
        }
        var trekkdager = venstreVerdi.annenPartEøs().trekkdager().verdi().add(høyre.getValue().annenPartEøs().trekkdager().verdi());
        var eøsUttak = new EøsUttakDto(venstreVerdi.annenPartEøs().kontoType(), new EøsUttakDto.Trekkdager(trekkdager));
        return nyttSegment(intervall, null, null, eøsUttak);
    }

    static LocalDateTimeline<UttakDto> tilTimeline(List<Planperiode> uttak) {
        var segmenter = uttak.stream().map(periode -> new LocalDateSegment<>(periode.fom(), periode.tom(), periode.uttak())).toList();
        return segmenter.isEmpty() ? LocalDateTimeline.empty() : new LocalDateTimeline<>(segmenter);
    }

    /**
     * Trekkdagene kopieres uendret til hvert segment perioden måtte bli delt i, slik
     * foreldrepengesøknad gjør i dag.
     * <p>
     * TODO: dette er feil. Splittes en EØS-periode på 5 trekkdager i to, får begge
     *  delene 5 trekkdager, og summen blir 10. En riktig løsning må fordele trekkdagene
     *  forholdsmessig etter virkedager og bevare totalen, noe som krever at segmentet husker
     *  hvilken periode det kom fra. Vi lever med feilen fordi EØS-perioder i praksis ikke
     *  overlapper med søkerens uttak, og dermed sjelden splittes. Oppstår overlapp i produksjon,
     *  er det denne koden som må skrives om.
     */
    static LocalDateTimeline<EøsUttakDto> tilEøsTimeline(List<UttakPeriodeAnnenpartEøs> perioder) {
        // TODO: Avklar om EØS-perioder skal virkedagsjusteres. Perioder som bare ligger i en helg kan være gyldige.
        var segmenter = perioder.stream()
            .map(periode -> new LocalDateSegment<>(periode.fom(), periode.tom(), UttaksplanMapper.mapEøsUttak(periode)))
            .toList();
        return segmenter.isEmpty() ? LocalDateTimeline.empty() : new LocalDateTimeline<>(segmenter);
    }
}
