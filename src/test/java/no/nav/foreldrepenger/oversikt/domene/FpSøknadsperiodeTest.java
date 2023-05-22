package no.nav.foreldrepenger.oversikt.domene;

import static no.nav.foreldrepenger.common.innsyn.Arbeidsgiver.ArbeidsgiverType;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.common.innsyn.Aktivitet;
import no.nav.foreldrepenger.common.innsyn.KontoType;

class FpSøknadsperiodeTest {

    @Test
    void mapper_til_dto() {
        var periode = new FpSøknadsperiode(LocalDate.now(), LocalDate.now().plusDays(5), Konto.MØDREKVOTE, UtsettelseÅrsak.SØKER_SYKDOM,
            OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER, OverføringÅrsak.SYKDOM_ANNEN_FORELDER,
            new Gradering(new Prosent(10), new UttakAktivitet(UttakAktivitet.Type.ORDINÆRT_ARBEID, Arbeidsgiver.dummy(), null)),
            new Prosent(20), true, MorsAktivitet.ARBEID);

        var dto = periode.tilDto();
        assertThat(dto.fom()).isEqualTo(periode.fom());
        assertThat(dto.fom()).isEqualTo(periode.fom());
        assertThat(dto.gradering().arbeidstidprosent().value()).isEqualTo(periode.gradering().prosent().decimalValue());
        assertThat(dto.gradering().aktivitet().type()).isEqualTo(Aktivitet.Type.ORDINÆRT_ARBEID);
        assertThat(dto.gradering().aktivitet().arbeidsgiver().type()).isEqualTo(ArbeidsgiverType.ORGANISASJON);
        assertThat(dto.gradering().aktivitet().arbeidsgiver().id()).isEqualTo(periode.gradering().uttakAktivitet().arbeidsgiver().identifikator());
        assertThat(dto.kontoType()).isEqualTo(KontoType.MØDREKVOTE);
        assertThat(dto.morsAktivitet()).isEqualTo(no.nav.foreldrepenger.common.innsyn.MorsAktivitet.ARBEID);
        assertThat(dto.resultat()).isNull();
        assertThat(dto.flerbarnsdager()).isTrue();
        assertThat(dto.samtidigUttak().value()).isEqualTo(periode.samtidigUttak().decimalValue());
        assertThat(dto.oppholdÅrsak()).isEqualTo(no.nav.foreldrepenger.common.innsyn.OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER);
        assertThat(dto.utsettelseÅrsak()).isEqualTo(no.nav.foreldrepenger.common.innsyn.UtsettelseÅrsak.SØKER_SYKDOM);
        assertThat(dto.overføringÅrsak()).isEqualTo(no.nav.foreldrepenger.common.innsyn.OverføringÅrsak.SYKDOM_ANNEN_FORELDER);
    }
}
