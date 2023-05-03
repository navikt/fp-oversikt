package no.nav.foreldrepenger.oversikt.domene;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class SakFP0TilDtoMapperTest {

    @Test
    void verifiser_at_gjeldende_vedtak_er_det_med_senest_vedtakstidspunkt() {
        var uttaksperioderGjeldendeVedtak = List.of(new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(1)));
        var vedtakene = Set.of(
            new Vedtak(LocalDateTime.now().minusYears(1), new Uttak(Dekningsgrad.HUNDRE, List.of(
                new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(1)),
                new Uttaksperiode(LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(2))
            ))),
            new Vedtak(LocalDateTime.now(), new Uttak(Dekningsgrad.ÅTTI, uttaksperioderGjeldendeVedtak
            ))
        );
        var sakFP0 = new SakFP0(new Saksnummer("1"), new AktørId("123"), vedtakene);

        var fpSakDto = sakFP0.tilSakDto();

        assertThat(fpSakDto.gjeldendeVedtak()).isNotNull();
        assertThat(fpSakDto.gjeldendeVedtak().perioder()).hasSameSizeAs(uttaksperioderGjeldendeVedtak);
        assertThat(fpSakDto.gjeldendeVedtak().perioder().get(0).fom()).isEqualTo(uttaksperioderGjeldendeVedtak.get(0).fom());
    }

    @Test
    void sjekk_at_mapping_av_uttaksperiode_til_dto_fungere() {
        var uttaksperiode = new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(1));

        var uttaksperiodeDto = uttaksperiode.tilDto();

        assertThat(uttaksperiodeDto.fom()).isEqualTo(uttaksperiode.fom());
        assertThat(uttaksperiodeDto.tom()).isEqualTo(uttaksperiode.tom());
        assertThat(uttaksperiodeDto.fom()).isBefore(uttaksperiode.tom());
    }

    @Test
    void sjekk_at_mapping_av_vedtak_til_dto_fungere_happy_case() {
        var uttaksperioder = List.of(
            new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(1)),
            new Uttaksperiode(LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(2))
        );
        var uttak = new Uttak(Dekningsgrad.HUNDRE, uttaksperioder);
        var vedtak = new Vedtak(LocalDateTime.now(), uttak);

        var vedtakDto = vedtak.tilDto();

        assertThat(vedtakDto.perioder()).hasSameSizeAs(vedtak.uttak().perioder());
    }


    @Test
    void sjekk_at_mapping_av_vedtak_til_dto_ikke_kaster_exception_når_uttak_er_null() {
        var uttak = new Uttak(Dekningsgrad.HUNDRE, null);
        var vedtak = new Vedtak(LocalDateTime.now(), uttak);

        var vedtakDto = vedtak.tilDto();

        assertThat(vedtakDto).isNull();
    }

}
