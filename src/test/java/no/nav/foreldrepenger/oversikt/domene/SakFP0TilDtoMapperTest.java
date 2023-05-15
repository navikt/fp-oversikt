package no.nav.foreldrepenger.oversikt.domene;

import static no.nav.foreldrepenger.oversikt.domene.Uttaksperiode.Resultat.Type.AVSLÅTT;
import static no.nav.foreldrepenger.oversikt.domene.Uttaksperiode.Resultat.Type.INNVILGET;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import no.nav.foreldrepenger.common.innsyn.BehandlingTilstand;

class SakFP0TilDtoMapperTest {

    @Test
    void verifiser_at_gjeldende_vedtak_er_det_med_senest_vedtakstidspunkt() {
        var uttaksperioderGjeldendeVedtak = List.of(new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(1), new Uttaksperiode.Resultat(
            INNVILGET)));
        var vedtakene = Set.of(
            new FpVedtak(LocalDateTime.now().minusYears(1), List.of(
                new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(1), new Uttaksperiode.Resultat(INNVILGET)),
                new Uttaksperiode(LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(2), new Uttaksperiode.Resultat(INNVILGET))
            ), Dekningsgrad.HUNDRE),
            new FpVedtak(LocalDateTime.now(), uttaksperioderGjeldendeVedtak, Dekningsgrad.ÅTTI)
        );
        var sakFP0 = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), SakStatus.UNDER_BEHANDLING, vedtakene, AktørId.dummy(), fh(), aksjonspunkt(),
            Set.of());

        var fnrAnnenPart = UUID.randomUUID().toString();
        var fpSakDto = sakFP0.tilSakDto(aktørId -> fnrAnnenPart);

        assertThat(fpSakDto.gjeldendeVedtak()).isNotNull();
        assertThat(fpSakDto.gjeldendeVedtak().perioder()).hasSameSizeAs(uttaksperioderGjeldendeVedtak);
        assertThat(fpSakDto.gjeldendeVedtak().perioder().get(0).fom()).isEqualTo(uttaksperioderGjeldendeVedtak.get(0).fom());
        assertThat(fpSakDto.annenPart().fnr().value()).isEqualTo(fnrAnnenPart);
    }

    private Set<Aksjonspunkt> aksjonspunkt() {
        return Set.of(new Aksjonspunkt("1234", Aksjonspunkt.Status.UTFØRT, "VENTER", LocalDateTime.now()));
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
        var vedtak = new FpVedtak(LocalDateTime.now(), uttaksperioder, Dekningsgrad.HUNDRE);

        var vedtakDto = vedtak.tilDto();

        assertThat(vedtakDto.perioder()).hasSameSizeAs(vedtak.perioder());
    }


    @Test
    void sjekk_at_mapping_av_vedtak_til_dto_ikke_kaster_exception_når_uttak_er_null() {
        var vedtak = new FpVedtak(LocalDateTime.now(), null, Dekningsgrad.HUNDRE);

        var vedtakDto = vedtak.tilDto();

        assertThat(vedtakDto.perioder()).isEmpty();
    }

    @Test
    void kan_søke_om_endring_hvis_periode_innvilget() {
        var vedtak = new FpVedtak(LocalDateTime.now().minusYears(1),
            List.of(new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(1), new Uttaksperiode.Resultat(INNVILGET)),
                new Uttaksperiode(LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(2), new Uttaksperiode.Resultat(AVSLÅTT))),
            Dekningsgrad.HUNDRE);
        var sakFP0 = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), SakStatus.UNDER_BEHANDLING, Set.of(vedtak), null, fh(), aksjonspunkt(),
            Set.of());

        var fpSakDto = sakFP0.tilSakDto(AktørId::value);

        assertThat(fpSakDto.kanSøkeOmEndring()).isTrue();
    }

    @Test
    void kan_ikke_søke_om_endring_hvis_alle_periodene_avslått() {
        var vedtak = new FpVedtak(LocalDateTime.now().minusYears(1),
            List.of(new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(1), new Uttaksperiode.Resultat(AVSLÅTT)),
                new Uttaksperiode(LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(2), new Uttaksperiode.Resultat(AVSLÅTT))),
            Dekningsgrad.HUNDRE);
        var sakFP0 = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), SakStatus.UNDER_BEHANDLING, Set.of(vedtak), null, fh(), aksjonspunkt(),
            Set.of());

        var fpSakDto = sakFP0.tilSakDto(AktørId::value);

        assertThat(fpSakDto.kanSøkeOmEndring()).isFalse();
    }

    @Test
    void kan_ikke_søke_om_endring_hvis_ingen_vedtak() {
        var sakFP0 = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), SakStatus.UNDER_BEHANDLING, Set.of(), null, fh(), aksjonspunkt(),
            Set.of());

        var fpSakDto = sakFP0.tilSakDto(AktørId::value);

        assertThat(fpSakDto.kanSøkeOmEndring()).isFalse();
    }

    @Test
    void skal_mappe_familieHendelse() {
        var familieHendelse = fh();
        var sakFP0 = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), SakStatus.UNDER_BEHANDLING, Set.of(), null, familieHendelse, aksjonspunkt(),
            Set.of());

        var fpSakDto = sakFP0.tilSakDto(AktørId::value);

        assertThat(fpSakDto.familiehendelse().antallBarn()).isEqualTo(familieHendelse.antallBarn());
        assertThat(fpSakDto.familiehendelse().fødselsdato()).isEqualTo(familieHendelse.fødselsdato());
        assertThat(fpSakDto.familiehendelse().termindato()).isEqualTo(familieHendelse.termindato());
        assertThat(fpSakDto.familiehendelse().omsorgsovertakelse()).isEqualTo(familieHendelse.omsorgsovertakelse());
    }

    @Test
    void skal_mappe_aksjonspunkt_og_søknad_til_åpen_behandling() {
        var familieHendelse = fh();
        var åpenBehandling = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), SakStatus.UNDER_BEHANDLING, Set.of(), null, familieHendelse,
            Set.of(new Aksjonspunkt(BehandlingTilstandUtleder.VENT_PGA_FOR_TIDLIG_SØKNAD, Aksjonspunkt.Status.OPPRETTET, null, LocalDateTime.now())),
            Set.of(new FpSøknad(SøknadStatus.MOTTATT, LocalDateTime.now(), Set.of())));
        var ikkeÅpenBehandling = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), SakStatus.UNDER_BEHANDLING, Set.of(), null, familieHendelse,
            Set.of(new Aksjonspunkt(BehandlingTilstandUtleder.VENT_PGA_FOR_TIDLIG_SØKNAD, Aksjonspunkt.Status.OPPRETTET, null, LocalDateTime.now())),
            Set.of(new FpSøknad(SøknadStatus.BEHANDLET, LocalDateTime.now(), Set.of())));


        var fpSakDto1 = åpenBehandling.tilSakDto(AktørId::value);
        var fpSakDto2 = ikkeÅpenBehandling.tilSakDto(AktørId::value);

        assertThat(fpSakDto1.åpenBehandling().tilstand()).isEqualTo(BehandlingTilstand.VENT_TIDLIG_SØKNAD);
        assertThat(fpSakDto2.åpenBehandling()).isNull();
    }

    @ParameterizedTest
    @EnumSource(SakStatus.class)
    void skal_mappe_status(SakStatus status) {
        var familieHendelse = fh();
        var sakFP0 = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), status, Set.of(), null, familieHendelse, aksjonspunkt(), Set.of());
        var fpSakDto = sakFP0.tilSakDto(AktørId::value);
        assertThat(fpSakDto.sakAvsluttet()).isEqualTo(status == SakStatus.AVSLUTTET);
    }

    private static FamilieHendelse fh() {
        return new FamilieHendelse(LocalDate.now(), LocalDate.now().plusDays(1), 1, LocalDate.now().plusDays(2));
    }
}
