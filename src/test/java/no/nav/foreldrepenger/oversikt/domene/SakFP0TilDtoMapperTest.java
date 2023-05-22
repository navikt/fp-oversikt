package no.nav.foreldrepenger.oversikt.domene;

import static java.util.Set.of;
import static no.nav.foreldrepenger.oversikt.domene.BrukerRolle.FAR;
import static no.nav.foreldrepenger.oversikt.domene.BrukerRolle.MEDMOR;
import static no.nav.foreldrepenger.oversikt.domene.BrukerRolle.MOR;
import static no.nav.foreldrepenger.oversikt.domene.Konto.FORELDREPENGER;
import static no.nav.foreldrepenger.oversikt.domene.Konto.MØDREKVOTE;
import static no.nav.foreldrepenger.oversikt.domene.Trekkdager.ZERO;
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

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.common.innsyn.BehandlingTilstand;
import no.nav.foreldrepenger.common.innsyn.Person;
import no.nav.foreldrepenger.common.innsyn.RettighetType;
import no.nav.foreldrepenger.oversikt.domene.Uttaksperiode.UttaksperiodeAktivitet;

class SakFP0TilDtoMapperTest {

    @Test
    void verifiser_at_gjeldende_vedtak_er_det_med_senest_vedtakstidspunkt() {
        var uttaksperioderGjeldendeVedtak = List.of(
            new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(1), new Uttaksperiode.Resultat(INNVILGET, uttaksperiodeAktivitet(ZERO))));
        var vedtakene = of(new FpVedtak(LocalDateTime.now().minusYears(1), List.of(
                new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(1), new Uttaksperiode.Resultat(INNVILGET, uttaksperiodeAktivitet(ZERO))),
                new Uttaksperiode(LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(2),
                    new Uttaksperiode.Resultat(INNVILGET, uttaksperiodeAktivitet(ZERO)))), Dekningsgrad.HUNDRE),
            new FpVedtak(LocalDateTime.now(), uttaksperioderGjeldendeVedtak, Dekningsgrad.ÅTTI));
        var sakFP0 = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), SakStatus.UNDER_BEHANDLING, vedtakene, AktørId.dummy(), fh(), of(),
            of(), MEDMOR, of(), rettigheter());

        var fnrAnnenPart = randomFnr();
        var fpSakDto = sakFP0.tilSakDto(aktørId -> fnrAnnenPart);

        assertThat(fpSakDto.gjeldendeVedtak()).isNotNull();
        assertThat(fpSakDto.gjeldendeVedtak().perioder()).hasSameSizeAs(uttaksperioderGjeldendeVedtak);
        assertThat(fpSakDto.gjeldendeVedtak().perioder().get(0).fom()).isEqualTo(uttaksperioderGjeldendeVedtak.get(0).fom());
        assertThat(fpSakDto.annenPart().fnr().value()).isEqualTo(fnrAnnenPart);
    }

    private static Set<UttaksperiodeAktivitet> uttaksperiodeAktivitet(Trekkdager trekkdager) {
        return uttaksperiodeAktivitet(trekkdager, MØDREKVOTE);
    }

    private static Set<UttaksperiodeAktivitet> uttaksperiodeAktivitet(Trekkdager trekkdager, Konto konto) {
        return Set.of(
            new UttaksperiodeAktivitet(new UttakAktivitet(UttakAktivitet.Type.ORDINÆRT_ARBEID, Arbeidsgiver.dummy(), UUID.randomUUID().toString()),
                konto, trekkdager, Prosent.ZERO));
    }

    @Test
    void sjekk_at_mapping_av_uttaksperiode_til_dto_fungere() {
        var uttaksperiode = new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(1),
            new Uttaksperiode.Resultat(INNVILGET, uttaksperiodeAktivitet(ZERO)));

        var uttaksperiodeDto = uttaksperiode.tilDto();

        assertThat(uttaksperiodeDto.fom()).isEqualTo(uttaksperiode.fom());
        assertThat(uttaksperiodeDto.tom()).isEqualTo(uttaksperiode.tom());
        assertThat(uttaksperiodeDto.fom()).isBefore(uttaksperiode.tom());
        assertThat(uttaksperiodeDto.resultat().innvilget()).isTrue();
    }

    @Test
    void sjekk_at_mapping_av_vedtak_til_dto_fungere_happy_case() {
        var uttaksperioder = List.of(new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(1),
                new Uttaksperiode.Resultat(INNVILGET, uttaksperiodeAktivitet(ZERO))),
            new Uttaksperiode(LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(2),
                new Uttaksperiode.Resultat(INNVILGET, uttaksperiodeAktivitet(ZERO))));
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
        var vedtak = new FpVedtak(LocalDateTime.now().minusYears(1), List.of(
            new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(1),
                new Uttaksperiode.Resultat(INNVILGET, uttaksperiodeAktivitet(ZERO))),
            new Uttaksperiode(LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(2),
                new Uttaksperiode.Resultat(AVSLÅTT, uttaksperiodeAktivitet(ZERO)))), Dekningsgrad.HUNDRE);
        var sakFP0 = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), SakStatus.UNDER_BEHANDLING, of(vedtak), null, fh(), of(), of(), MOR,
            of(), rettigheter());

        var fpSakDto = sakFP0.tilSakDto(AktørId::value);

        assertThat(fpSakDto.kanSøkeOmEndring()).isTrue();
    }

    private Rettigheter rettigheter() {
        return new Rettigheter(false, false, false);
    }

    @Test
    void kan_ikke_søke_om_endring_hvis_alle_periodene_avslått() {
        var vedtak = new FpVedtak(LocalDateTime.now().minusYears(1), List.of(
            new Uttaksperiode(LocalDate.now(), LocalDate.now().plusMonths(1),
                new Uttaksperiode.Resultat(AVSLÅTT, uttaksperiodeAktivitet(ZERO))),
            new Uttaksperiode(LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(2),
                new Uttaksperiode.Resultat(AVSLÅTT, uttaksperiodeAktivitet(ZERO)))), Dekningsgrad.HUNDRE);
        var sakFP0 = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), SakStatus.UNDER_BEHANDLING, of(vedtak), null, fh(), of(), of(), MOR,
            of(), rettigheter());

        var fpSakDto = sakFP0.tilSakDto(AktørId::value);

        assertThat(fpSakDto.kanSøkeOmEndring()).isFalse();
    }

    @Test
    void kan_ikke_søke_om_endring_hvis_ingen_vedtak() {
        var sakFP0 = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), SakStatus.UNDER_BEHANDLING, of(), null, fh(), of(), of(), FAR, of(),
            rettigheter());

        var fpSakDto = sakFP0.tilSakDto(AktørId::value);

        assertThat(fpSakDto.kanSøkeOmEndring()).isFalse();
    }

    @Test
    void skal_mappe_familieHendelse() {
        var familieHendelse = fh();
        var sakFP0 = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), SakStatus.UNDER_BEHANDLING, of(), null, familieHendelse, of(), of(),
            MOR, of(), rettigheter());

        var fpSakDto = sakFP0.tilSakDto(AktørId::value);

        assertThat(fpSakDto.familiehendelse().antallBarn()).isEqualTo(familieHendelse.antallBarn());
        assertThat(fpSakDto.familiehendelse().fødselsdato()).isEqualTo(familieHendelse.fødselsdato());
        assertThat(fpSakDto.familiehendelse().termindato()).isEqualTo(familieHendelse.termindato());
        assertThat(fpSakDto.familiehendelse().omsorgsovertakelse()).isEqualTo(familieHendelse.omsorgsovertakelse());
    }

    @Test
    void skal_mappe_aksjonspunkt_og_søknad_til_åpen_behandling() {
        var familieHendelse = fh();
        var åpenBehandling = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), SakStatus.UNDER_BEHANDLING, of(), null, familieHendelse,
            of(new Aksjonspunkt(Aksjonspunkt.Type.VENT_TIDLIG_SØKNAD, null, LocalDateTime.now())),
            of(new FpSøknad(SøknadStatus.MOTTATT, LocalDateTime.now(), of())), MOR, of(), rettigheter());
        var ikkeÅpenBehandling = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), SakStatus.UNDER_BEHANDLING, of(), null, familieHendelse,
            of(new Aksjonspunkt(Aksjonspunkt.Type.VENT_TIDLIG_SØKNAD, null, LocalDateTime.now())),
            of(new FpSøknad(SøknadStatus.BEHANDLET, LocalDateTime.now(), of())), MOR, of(), rettigheter());


        var fpSakDto1 = åpenBehandling.tilSakDto(AktørId::value);
        var fpSakDto2 = ikkeÅpenBehandling.tilSakDto(AktørId::value);

        assertThat(fpSakDto1.åpenBehandling().tilstand()).isEqualTo(BehandlingTilstand.VENT_TIDLIG_SØKNAD);
        assertThat(fpSakDto2.åpenBehandling()).isNull();
    }

    @Test
    void skal_utlede_om_sak_er_mors_basert_på_bruker_rolle() {
        var mor = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), SakStatus.UNDER_BEHANDLING, of(), null, fh(), of(), of(), MOR, of(), rettigheter());
        var far = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), SakStatus.UNDER_BEHANDLING, of(), null, fh(), of(), of(), FAR, of(), rettigheter());
        var medmor = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), SakStatus.UNDER_BEHANDLING, of(), null, fh(), of(), of(), MEDMOR, of(),
            rettigheter());

        assertThat(mor.tilSakDto(aktørId -> "").sakTilhørerMor()).isTrue();
        assertThat(far.tilSakDto(aktørId -> "").sakTilhørerMor()).isFalse();
        assertThat(medmor.tilSakDto(aktørId -> "").sakTilhørerMor()).isFalse();
    }

    @Test
    void skal_mappe_barn() {
        var barn1AktørId = AktørId.dummy();
        var barn2AktørId = AktørId.dummy();
        var sak = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), SakStatus.UNDER_BEHANDLING, of(), null, fh(), of(), of(), MOR,
            of(barn1AktørId, barn2AktørId), rettigheter());

        var dto = sak.tilSakDto(AktørId::value);
        assertThat(dto.barn()).containsExactlyInAnyOrder(new Person(new Fødselsnummer(barn1AktørId.value()), null),
            new Person(new Fødselsnummer(barn2AktørId.value()), null));
    }

    @Test
    void skal_mappe_rettigheter_begge_rett() {
        var uttaksperioder = List.of(new Uttaksperiode(LocalDate.now(), LocalDate.now(),
            new Uttaksperiode.Resultat(INNVILGET, uttaksperiodeAktivitet(new Trekkdager(20), MØDREKVOTE))));
        var vedtak = new FpVedtak(LocalDateTime.now(), uttaksperioder, Dekningsgrad.HUNDRE);
        var sak = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), SakStatus.UNDER_BEHANDLING, Set.of(vedtak), null, fh(), of(), of(), MOR, of(),
            new Rettigheter(false, false, false));

        var dto = sak.tilSakDto(AktørId::value);
        assertThat(dto.rettighetType()).isEqualTo(RettighetType.BEGGE_RETT);
        assertThat(dto.morUføretrygd()).isFalse();
        assertThat(dto.harAnnenForelderTilsvarendeRettEØS()).isFalse();
    }

    @Test
    void skal_mappe_rettigheter_aleneomsorg() {
        var uttaksperioder = List.of(new Uttaksperiode(LocalDate.now(), LocalDate.now(),
            new Uttaksperiode.Resultat(INNVILGET, uttaksperiodeAktivitet(new Trekkdager(20), FORELDREPENGER))));
        var vedtak = new FpVedtak(LocalDateTime.now(), uttaksperioder, Dekningsgrad.HUNDRE);
        var sak = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), SakStatus.UNDER_BEHANDLING, Set.of(vedtak), null, fh(), of(), of(), MOR, of(),
            new Rettigheter(true, false, false));

        var dto = sak.tilSakDto(AktørId::value);
        assertThat(dto.rettighetType()).isEqualTo(RettighetType.ALENEOMSORG);
        assertThat(dto.morUføretrygd()).isFalse();
        assertThat(dto.harAnnenForelderTilsvarendeRettEØS()).isFalse();
    }

    @Test
    void skal_mappe_rettigheter_enerett() {
        var uttaksperioder = List.of(new Uttaksperiode(LocalDate.now(), LocalDate.now(),
            new Uttaksperiode.Resultat(INNVILGET, uttaksperiodeAktivitet(new Trekkdager(20), FORELDREPENGER))));
        var vedtak = new FpVedtak(LocalDateTime.now(), uttaksperioder, Dekningsgrad.HUNDRE);
        var sak = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), SakStatus.UNDER_BEHANDLING, Set.of(vedtak), null, fh(), of(), of(), MOR, of(),
            new Rettigheter(false, true, true));

        var dto = sak.tilSakDto(AktørId::value);
        assertThat(dto.rettighetType()).isEqualTo(RettighetType.BARE_SØKER_RETT);
        assertThat(dto.morUføretrygd()).isTrue();
        assertThat(dto.harAnnenForelderTilsvarendeRettEØS()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(SakStatus.class)
    void skal_mappe_status(SakStatus status) {
        var familieHendelse = fh();
        var sakFP0 = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), status, of(), null, familieHendelse, of(), of(), FAR, of(),
            rettigheter());
        var fpSakDto = sakFP0.tilSakDto(AktørId::value);
        assertThat(fpSakDto.sakAvsluttet()).isEqualTo(status == SakStatus.AVSLUTTET);
    }

    private static String randomFnr() {
        return UUID.randomUUID().toString();
    }

    private static FamilieHendelse fh() {
        return new FamilieHendelse(LocalDate.now(), LocalDate.now().plusDays(1), 1, LocalDate.now().plusDays(2));
    }
}
