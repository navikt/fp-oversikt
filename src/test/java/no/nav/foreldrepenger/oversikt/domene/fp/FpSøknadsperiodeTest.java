package no.nav.foreldrepenger.oversikt.domene.fp;

import static no.nav.foreldrepenger.common.innsyn.Arbeidsgiver.ArbeidsgiverType;
import static no.nav.foreldrepenger.common.innsyn.BrukerRolleSak.FAR_MEDMOR;
import static no.nav.foreldrepenger.common.innsyn.BrukerRolleSak.MOR;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.common.innsyn.Aktivitet;
import no.nav.foreldrepenger.common.innsyn.KontoType;
import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;
import no.nav.foreldrepenger.oversikt.domene.Prosent;

class FpSøknadsperiodeTest {

    @Test
    void mapper_til_dto() {
        var periode = periode();

        var dto = periode.tilDto(MOR);
        assertThat(dto.fom()).isEqualTo(periode.fom());
        assertThat(dto.fom()).isEqualTo(periode.fom());
        assertThat(dto.gradering().arbeidstidprosent().value()).isEqualTo(periode.gradering().prosent().decimalValue());
        assertThat(dto.gradering().aktivitet().type()).isEqualTo(Aktivitet.AktivitetType.ORDINÆRT_ARBEID);
        assertThat(dto.gradering().aktivitet().arbeidsgiver().type()).isEqualTo(ArbeidsgiverType.ORGANISASJON);
        assertThat(dto.gradering().aktivitet().arbeidsgiver().id()).isEqualTo(periode.gradering().uttakAktivitet().arbeidsgiver().identifikator());
        assertThat(dto.kontoType()).isEqualTo(KontoType.MØDREKVOTE);
        assertThat(dto.morsAktivitet()).isEqualTo(no.nav.foreldrepenger.common.innsyn.MorsAktivitet.ARBEID);
        assertThat(dto.resultat()).isNull();
        assertThat(dto.flerbarnsdager()).isTrue();
        assertThat(dto.samtidigUttak()).isNull();
        assertThat(dto.oppholdÅrsak()).isEqualTo(no.nav.foreldrepenger.common.innsyn.UttakOppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER);
        assertThat(dto.utsettelseÅrsak()).isEqualTo(no.nav.foreldrepenger.common.innsyn.UttakUtsettelseÅrsak.SØKER_SYKDOM);
        assertThat(dto.overføringÅrsak()).isEqualTo(no.nav.foreldrepenger.common.innsyn.UttakOverføringÅrsak.SYKDOM_ANNEN_FORELDER);
        assertThat(dto.forelder()).isEqualTo(MOR);
    }

    @Test
    void samtidig_uttak_skal_sett_gradering_til_null() {
        var periode = periode(new Prosent(10), new Prosent(20));

        var dto = periode.tilDto(MOR);

        assertThat(dto.gradering()).isNull();
        assertThat(dto.samtidigUttak().value()).isEqualTo(periode.samtidigUttak().decimalValue());
    }

    @Test
    void null_prosent_samtidig_uttak_er_ikke_samtidig_uttak() {
        var periode = periode(Prosent.ZERO, null);

        var dto = periode.tilDto(FAR_MEDMOR);

        assertThat(dto.samtidigUttak()).isNull();
    }

    private static FpSøknadsperiode periode() {
        return periode(null, new Prosent(10));
    }

    private static FpSøknadsperiode periode(Prosent samtidigUttak, Prosent arbeidsprosent) {
        var gradering = arbeidsprosent == null ? null : new Gradering(arbeidsprosent, new UttakAktivitet(UttakAktivitet.Type.ORDINÆRT_ARBEID, Arbeidsgiver.dummy(), null));
        return new FpSøknadsperiode(LocalDate.now(), LocalDate.now().plusDays(5), Konto.MØDREKVOTE, UtsettelseÅrsak.SØKER_SYKDOM,
            OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER, OverføringÅrsak.SYKDOM_ANNEN_FORELDER, gradering, samtidigUttak, true,
            MorsAktivitet.ARBEID);
    }
}
