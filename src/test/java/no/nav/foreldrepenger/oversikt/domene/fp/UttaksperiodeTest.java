package no.nav.foreldrepenger.oversikt.domene.fp;

import static no.nav.foreldrepenger.common.innsyn.BrukerRolleSak.FAR_MEDMOR;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.common.innsyn.Aktivitet;
import no.nav.foreldrepenger.common.innsyn.KontoType;
import no.nav.foreldrepenger.common.innsyn.UttakPeriodeResultat;
import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;
import no.nav.foreldrepenger.oversikt.domene.Prosent;

class UttaksperiodeTest {

    @Test
    void mapper_til_dto() {
        var periode = periode(null, new Prosent(10), Uttaksperiode.Resultat.Type.INNVILGET_GRADERING);

        var dto = periode.tilDto(FAR_MEDMOR);
        assertThat(dto.fom()).isEqualTo(periode.fom());
        assertThat(dto.fom()).isEqualTo(periode.fom());
        var uttaksperiodeAktivitet = periode.resultat().aktiviteter().stream().findFirst().orElseThrow();
        assertThat(dto.gradering().arbeidstidprosent().value()).isEqualTo(uttaksperiodeAktivitet.arbeidstidsprosent().decimalValue());
        assertThat(dto.gradering().aktivitet().type()).isEqualTo(Aktivitet.AktivitetType.ORDINÆRT_ARBEID);
        assertThat(dto.gradering().aktivitet().arbeidsgiver().type()).isEqualTo(no.nav.foreldrepenger.common.innsyn.Arbeidsgiver.ArbeidsgiverType.ORGANISASJON);
        assertThat(dto.gradering().aktivitet().arbeidsgiver().id()).isEqualTo(uttaksperiodeAktivitet.aktivitet().arbeidsgiver().identifikator());
        assertThat(dto.kontoType()).isEqualTo(KontoType.MØDREKVOTE);
        assertThat(dto.morsAktivitet()).isEqualTo(no.nav.foreldrepenger.common.innsyn.MorsAktivitet.ARBEID);
        assertThat(dto.flerbarnsdager()).isTrue();
        assertThat(dto.samtidigUttak()).isNull();
        assertThat(dto.oppholdÅrsak()).isEqualTo(no.nav.foreldrepenger.common.innsyn.UttakOppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER);
        assertThat(dto.utsettelseÅrsak()).isEqualTo(no.nav.foreldrepenger.common.innsyn.UttakUtsettelseÅrsak.SØKER_SYKDOM);
        assertThat(dto.overføringÅrsak()).isEqualTo(no.nav.foreldrepenger.common.innsyn.UttakOverføringÅrsak.SYKDOM_ANNEN_FORELDER);
        assertThat(dto.resultat().trekkerDager()).isTrue();
        assertThat(dto.resultat().trekkerMinsterett()).isTrue();
        assertThat(dto.resultat().årsak()).isEqualTo(UttakPeriodeResultat.UttakPeriodeResultatÅrsak.ANNET);
        assertThat(dto.forelder()).isEqualTo(FAR_MEDMOR);
    }

    @Test
    void samtidig_uttak_skal_sett_gradering_til_null() {
        var periode = periode(new Prosent(10), new Prosent(20), Uttaksperiode.Resultat.Type.INNVILGET);

        var dto = periode.tilDto(FAR_MEDMOR);

        assertThat(dto.gradering()).isNull();
        assertThat(dto.samtidigUttak().value()).isEqualTo(periode.samtidigUttak().decimalValue());
    }

    @Test
    void null_prosent_samtidig_uttak_er_ikke_samtidig_uttak() {
        var periode = periode(Prosent.ZERO, Prosent.ZERO, Uttaksperiode.Resultat.Type.INNVILGET);

        var dto = periode.tilDto(FAR_MEDMOR);

        assertThat(dto.samtidigUttak()).isNull();
    }

    private static Uttaksperiode periode(Prosent samtidigUttak, Prosent arbeidsprosent, Uttaksperiode.Resultat.Type type) {
        return new Uttaksperiode(LocalDate.now(), LocalDate.now().plusDays(5), UtsettelseÅrsak.SØKER_SYKDOM,
            OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER, OverføringÅrsak.SYKDOM_ANNEN_FORELDER, samtidigUttak, true,
            MorsAktivitet.ARBEID, new Uttaksperiode.Resultat(type, Uttaksperiode.Resultat.Årsak.ANNET, Set.of(
                new Uttaksperiode.UttaksperiodeAktivitet(new UttakAktivitet(UttakAktivitet.Type.ORDINÆRT_ARBEID, Arbeidsgiver.dummy(), null),
                    Konto.MØDREKVOTE, new Trekkdager(10), arbeidsprosent)
        ), true));
    }

}
