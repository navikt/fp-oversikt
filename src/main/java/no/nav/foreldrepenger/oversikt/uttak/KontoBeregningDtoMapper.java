package no.nav.foreldrepenger.oversikt.uttak;

import static no.nav.foreldrepenger.stønadskonto.regelmodell.StønadskontoKontotype.BARE_FAR_RETT;
import static no.nav.foreldrepenger.stønadskonto.regelmodell.StønadskontoKontotype.FAR_RUNDT_FØDSEL;
import static no.nav.foreldrepenger.stønadskonto.regelmodell.StønadskontoKontotype.FLERBARNSDAGER;
import static no.nav.foreldrepenger.stønadskonto.regelmodell.StønadskontoKontotype.FORELDREPENGER;
import static no.nav.foreldrepenger.stønadskonto.regelmodell.StønadskontoKontotype.TETTE_SAKER_FAR;
import static no.nav.foreldrepenger.stønadskonto.regelmodell.StønadskontoKontotype.TETTE_SAKER_MOR;
import static no.nav.foreldrepenger.stønadskonto.regelmodell.StønadskontoKontotype.TILLEGG_FLERBARN;
import static no.nav.foreldrepenger.stønadskonto.regelmodell.StønadskontoKontotype.TILLEGG_PREMATUR;
import static no.nav.foreldrepenger.stønadskonto.regelmodell.StønadskontoKontotype.UFØREDAGER;
import static no.nav.foreldrepenger.stønadskonto.regelmodell.grunnlag.Brukerrolle.FAR;
import static no.nav.foreldrepenger.stønadskonto.regelmodell.grunnlag.Brukerrolle.MEDMOR;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.stønadskonto.regelmodell.StønadskontoKontotype;
import no.nav.foreldrepenger.stønadskonto.regelmodell.grunnlag.Brukerrolle;
import no.nav.foreldrepenger.stønadskonto.regelmodell.grunnlag.Rettighetstype;

public class KontoBeregningDtoMapper {

    private KontoBeregningDtoMapper() {
        // Statisk implementasjon
    }

    public static KontoBeregningDto tilKontoberegning(Map<StønadskontoKontotype, Integer> stønadskontoer, KontoBeregningGrunnlagDto grunnlag) {
        return new KontoBeregningDto(tilKontoer(stønadskontoer, grunnlag),
            tilMinsteretter(stønadskontoer, grunnlag.brukerrolle()),
            tilTillegg(stønadskontoer));
    }

    private static List<KontoBeregningDto.KontoDto> tilKontoer(Map<StønadskontoKontotype, Integer> stønadskontoer, KontoBeregningGrunnlagDto grunnlag) {
        var kontoer = new ArrayList<KontoBeregningDto.KontoDto>();
        if (Set.of(FAR, MEDMOR).contains(grunnlag.brukerrolle()) && grunnlag.rettighetstype().equals(Rettighetstype.BARE_SØKER_RETT)) {
            var dagerUtenAktivitetskrav = dagerUtenAktivitetskrav(stønadskontoer);
            kontoer.add(new KontoBeregningDto.KontoDto(KontoBeregningDto.KontoDto.KontoTypeUttak.AKTIVITETSFRI_KVOTE, dagerUtenAktivitetskrav));
            kontoer.add(tilKontoDto(StønadskontoKontotype.FORELDREPENGER, stønadskontoer.get(FORELDREPENGER) - dagerUtenAktivitetskrav));
        } else {
            stønadskontoer.entrySet()
                .stream()
                .filter(k -> tilKontoType(k.getKey()).isPresent())
                .map(k -> tilKontoDto(k.getKey(), k.getValue()))
                .forEach(kontoer::add);
        }
        return kontoer;
    }

    private static KontoBeregningDto.KontoDto tilKontoDto(StønadskontoKontotype kontotype, Integer verdi) {
        return new KontoBeregningDto.KontoDto(tilKontoType(kontotype).orElseThrow(), verdi);
    }

    private static KontoBeregningDto.Minsteretter tilMinsteretter(Map<StønadskontoKontotype, Integer> kontoer, Brukerrolle brukerrolle) {
        return new KontoBeregningDto.Minsteretter(kontoer.getOrDefault(FAR_RUNDT_FØDSEL, 0), toTetteFra(kontoer, brukerrolle));
    }

    private static KontoBeregningDto.Tillegg tilTillegg(Map<StønadskontoKontotype, Integer> kontoer) {
        return new KontoBeregningDto.Tillegg(kontoer.getOrDefault(TILLEGG_FLERBARN, 0), kontoer.getOrDefault(TILLEGG_PREMATUR, 0));
    }

    // Kalles bare ved BFHR
    private static int dagerUtenAktivitetskrav(Map<StønadskontoKontotype, Integer> k) {
        return k.getOrDefault(BARE_FAR_RETT, 0) + // BFHR ETTER WLB
            k.getOrDefault(UFØREDAGER, 0) + // BFHR FØR WLB
            k.getOrDefault(FLERBARNSDAGER, 0); // BFHR FØR WLB
    }

    private static Integer toTetteFra(Map<StønadskontoKontotype, Integer> kontoer, Brukerrolle brukerrolle) {
        if (brukerrolle.equals(Brukerrolle.MOR)) {
            return kontoer.getOrDefault(TETTE_SAKER_MOR, 0);
        } else {
            return kontoer.getOrDefault(TETTE_SAKER_FAR, 0);
        }
    }

    private static Optional<KontoBeregningDto.KontoDto.KontoTypeUttak> tilKontoType(StønadskontoKontotype konto) {
        return switch (konto) {
            case FELLESPERIODE -> Optional.of(KontoBeregningDto.KontoDto.KontoTypeUttak.FELLESPERIODE);
            case MØDREKVOTE -> Optional.of(KontoBeregningDto.KontoDto.KontoTypeUttak.MØDREKVOTE);
            case FEDREKVOTE -> Optional.of(KontoBeregningDto.KontoDto.KontoTypeUttak.FEDREKVOTE);
            case FORELDREPENGER -> Optional.of(KontoBeregningDto.KontoDto.KontoTypeUttak.FORELDREPENGER);
            case FORELDREPENGER_FØR_FØDSEL -> Optional.of(KontoBeregningDto.KontoDto.KontoTypeUttak.FORELDREPENGER_FØR_FØDSEL);
            default -> Optional.empty();
        };
    }
}
