package no.nav.foreldrepenger.oversikt.domene;

import static no.nav.foreldrepenger.oversikt.domene.Uttaksperiode.Resultat.Type.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SakFP0TilDtoMapperTest {

    @Test
    void verifiser_at_gjeldende_vedtak_er_det_med_senest_vedtakstidspunkt() {
        var uttaksperioderGjeldendeVedtak = List.of(new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(1), new Uttaksperiode.Resultat(
            INNVILGET)));
        var vedtakene = Set.of(
            new Vedtak(LocalDateTime.now().minusYears(1), List.of(
                new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(1), new Uttaksperiode.Resultat(INNVILGET)),
                new Uttaksperiode(LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(2), new Uttaksperiode.Resultat(INNVILGET))
            ), Dekningsgrad.HUNDRE),
            new Vedtak(LocalDateTime.now(), uttaksperioderGjeldendeVedtak, Dekningsgrad.ÅTTI)
        );
        var sakFP0 = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), vedtakene, AktørId.dummy(), fh());

        var fnrAnnenPart = UUID.randomUUID().toString();
        var fpSakDto = sakFP0.tilSakDto(aktørId -> fnrAnnenPart);

        assertThat(fpSakDto.gjeldendeVedtak()).isNotNull();
        assertThat(fpSakDto.gjeldendeVedtak().perioder()).hasSameSizeAs(uttaksperioderGjeldendeVedtak);
        assertThat(fpSakDto.gjeldendeVedtak().perioder().get(0).fom()).isEqualTo(uttaksperioderGjeldendeVedtak.get(0).fom());
        assertThat(fpSakDto.annenPart().fnr().value()).isEqualTo(fnrAnnenPart);
    }

    @Test
    void sjekk_at_mapping_av_uttaksperiode_til_dto_fungere() {
        var uttaksperiode = new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(1), new Uttaksperiode.Resultat(
            INNVILGET));

        var uttaksperiodeDto = uttaksperiode.tilDto();

        assertThat(uttaksperiodeDto.fom()).isEqualTo(uttaksperiode.fom());
        assertThat(uttaksperiodeDto.tom()).isEqualTo(uttaksperiode.tom());
        assertThat(uttaksperiodeDto.fom()).isBefore(uttaksperiode.tom());
        assertThat(uttaksperiodeDto.resultat().innvilget()).isTrue();
    }

    @Test
    void sjekk_at_mapping_av_vedtak_til_dto_fungere_happy_case() {
        var uttaksperioder = List.of(
            new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(1), new Uttaksperiode.Resultat(
                INNVILGET)),
            new Uttaksperiode(LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(2), new Uttaksperiode.Resultat(
                INNVILGET))
        );
        var vedtak = new Vedtak(LocalDateTime.now(), uttaksperioder, Dekningsgrad.HUNDRE);

        var vedtakDto = vedtak.tilDto();

        assertThat(vedtakDto.perioder()).hasSameSizeAs(vedtak.perioder());
    }


    @Test
    void sjekk_at_mapping_av_vedtak_til_dto_ikke_kaster_exception_når_uttak_er_null() {
        var vedtak = new Vedtak(LocalDateTime.now(), null, Dekningsgrad.HUNDRE);

        var vedtakDto = vedtak.tilDto();

        assertThat(vedtakDto.perioder()).isEmpty();
    }

    @Test
    void kan_søke_om_endring_hvis_periode_innvilget() {
        var vedtak = new Vedtak(LocalDateTime.now().minusYears(1),
            List.of(new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(1), new Uttaksperiode.Resultat(INNVILGET)),
                new Uttaksperiode(LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(2), new Uttaksperiode.Resultat(AVSLÅTT))),
            Dekningsgrad.HUNDRE);
        var sakFP0 = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), Set.of(vedtak), null, fh());

        var fpSakDto = sakFP0.tilSakDto(AktørId::value);

        assertThat(fpSakDto.kanSøkeOmEndring()).isTrue();
    }

    @Test
    void kan_ikke_søke_om_endring_hvis_alle_periodene_avslått() {
        var vedtak = new Vedtak(LocalDateTime.now().minusYears(1),
            List.of(new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(1), new Uttaksperiode.Resultat(AVSLÅTT)),
                new Uttaksperiode(LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(2), new Uttaksperiode.Resultat(AVSLÅTT))),
            Dekningsgrad.HUNDRE);
        var sakFP0 = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), Set.of(vedtak), null, fh());

        var fpSakDto = sakFP0.tilSakDto(AktørId::value);

        assertThat(fpSakDto.kanSøkeOmEndring()).isFalse();
    }

    @Test
    void kan_ikke_søke_om_endring_hvis_ingen_vedtak() {
        var sakFP0 = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), Set.of(), null, fh());

        var fpSakDto = sakFP0.tilSakDto(AktørId::value);

        assertThat(fpSakDto.kanSøkeOmEndring()).isFalse();
    }

    @Test
    void skal_mappe_familieHendelse() {
        var familieHendelse = fh();
        var sakFP0 = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), Set.of(), null, familieHendelse);

        var fpSakDto = sakFP0.tilSakDto(AktørId::value);

        assertThat(fpSakDto.familiehendelse().antallBarn()).isEqualTo(familieHendelse.antallBarn());
        assertThat(fpSakDto.familiehendelse().fødselsdato()).isEqualTo(familieHendelse.fødselsdato());
        assertThat(fpSakDto.familiehendelse().termindato()).isEqualTo(familieHendelse.termindato());
        assertThat(fpSakDto.familiehendelse().omsorgsovertakelse()).isEqualTo(familieHendelse.omsorgsovertakelse());
    }

    private static FamilieHendelse fh() {
        return new FamilieHendelse(LocalDate.now(), LocalDate.now().plusDays(1), 1, LocalDate.now().plusDays(2));
    }
}
