package no.nav.foreldrepenger.oversikt.domene.fp;

import static java.time.LocalDate.now;
import static java.util.Set.of;
import static no.nav.foreldrepenger.kontrakter.fpoversikt.BrukerRolleSak.FAR_MEDMOR;
import static no.nav.foreldrepenger.oversikt.domene.fp.BrukerRolle.FAR;
import static no.nav.foreldrepenger.oversikt.domene.fp.BrukerRolle.MEDMOR;
import static no.nav.foreldrepenger.oversikt.domene.fp.BrukerRolle.MOR;
import static no.nav.foreldrepenger.oversikt.domene.fp.Konto.FORELDREPENGER;
import static no.nav.foreldrepenger.oversikt.domene.fp.Konto.MØDREKVOTE;
import static no.nav.foreldrepenger.oversikt.domene.fp.Trekkdager.ZERO;
import static no.nav.foreldrepenger.oversikt.domene.fp.Uttaksperiode.Resultat.Type.AVSLÅTT;
import static no.nav.foreldrepenger.oversikt.domene.fp.Uttaksperiode.Resultat.Type.INNVILGET;
import static no.nav.foreldrepenger.oversikt.domene.fp.Uttaksperiode.Resultat.Årsak.ANNET;
import static no.nav.foreldrepenger.oversikt.stub.DummyPersonOppslagSystemTest.annenpartUbeskyttetAdresse;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.kontrakter.fpoversikt.BehandlingTilstand;
import no.nav.foreldrepenger.kontrakter.fpoversikt.Person;
import no.nav.foreldrepenger.kontrakter.fpoversikt.RettighetType;
import no.nav.foreldrepenger.oversikt.domene.Aksjonspunkt;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.Prosent;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.SøknadStatus;
import no.nav.foreldrepenger.oversikt.domene.fp.Uttaksperiode.UttaksperiodeAktivitet;

class SakFP0Test {

    @Test
    void verifiser_at_gjeldende_vedtak_er_det_med_senest_vedtakstidspunkt() {
        var uttaksperioderGjeldendeVedtak = List.of(uttaksperiode(now(), now().plusMonths(1), innvilget(ZERO)));
        var gjeldendeVedtak = new FpVedtak(LocalDateTime.now(), uttaksperioderGjeldendeVedtak, Dekningsgrad.ÅTTI, null);
        var tidligereVedtak = new FpVedtak(LocalDateTime.now().minusYears(1), List.of(uttaksperiode(now(), now().plusMonths(1), innvilget(ZERO)),
            uttaksperiode(now().plusMonths(1), now().plusMonths(2), innvilget(ZERO))), Dekningsgrad.HUNDRE, null);
        var vedtakene = of(tidligereVedtak, gjeldendeVedtak);
        var sakFP0 = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), false, vedtakene, AktørId.dummy(), fh(), of(),
            of(), MEDMOR, of(), rettigheter(), false, LocalDateTime.now());

        var annenpartOppslag = annenpartUbeskyttetAdresse();
        var fpSakDto = sakFP0.tilSakDto(annenpartOppslag);

        assertThat(fpSakDto.gjeldendeVedtak()).isNotNull();
        assertThat(fpSakDto.gjeldendeVedtak().perioder()).hasSameSizeAs(uttaksperioderGjeldendeVedtak);
        assertThat(fpSakDto.gjeldendeVedtak().perioder().getFirst().fom()).isEqualTo(uttaksperioderGjeldendeVedtak.getFirst().fom());
        assertThat(fpSakDto.annenPart().fnr().value()).isEqualTo(sakFP0.annenPartAktørId().value());
    }

    private static Uttaksperiode.Resultat innvilget(Trekkdager trekkdager) {
        return new Uttaksperiode.Resultat(INNVILGET, ANNET, uttaksperiodeAktivitet(trekkdager), false);
    }

    private static Uttaksperiode uttaksperiode(LocalDate fom, LocalDate tom, Uttaksperiode.Resultat resultat) {
        return new Uttaksperiode(fom, tom, null, null, null, null, false, null, resultat);
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
        var uttaksperiode = uttaksperiode(now(), now().plusMonths(1), innvilget(ZERO));

        var uttaksperiodeDto = uttaksperiode.tilDto(FAR_MEDMOR);

        assertThat(uttaksperiodeDto.fom()).isEqualTo(uttaksperiode.fom());
        assertThat(uttaksperiodeDto.tom()).isEqualTo(uttaksperiode.tom());
        assertThat(uttaksperiodeDto.fom()).isBefore(uttaksperiode.tom());
        assertThat(uttaksperiodeDto.resultat().innvilget()).isTrue();
    }

    @Test
    void sjekk_at_mapping_av_vedtak_til_dto_fungere_happy_case() {
        var uttaksperioder = List.of(uttaksperiode(LocalDate.of(2024, 10, 10), LocalDate.of(2024, 10, 10).plusMonths(1).minusDays(5), innvilget(ZERO)),
            uttaksperiode(LocalDate.of(2024, 10, 10).plusMonths(1), LocalDate.of(2024, 10, 10).plusMonths(2), innvilget(ZERO)));
        var vedtak = new FpVedtak(LocalDateTime.now(), uttaksperioder, Dekningsgrad.HUNDRE, null);

        var vedtakDto = vedtak.tilDto(no.nav.foreldrepenger.kontrakter.fpoversikt.BrukerRolleSak.MOR);

        assertThat(vedtakDto.perioder()).hasSameSizeAs(vedtak.perioder());
    }


    @Test
    void sjekk_at_mapping_av_vedtak_til_dto_ikke_kaster_exception_når_uttak_er_null() {
        var vedtak = new FpVedtak(LocalDateTime.now(), null, Dekningsgrad.HUNDRE, null);

        var vedtakDto = vedtak.tilDto(FAR_MEDMOR);

        assertThat(vedtakDto.perioder()).isEmpty();
    }

    @Test
    void kan_søke_om_endring_hvis_periode_innvilget() {
        var vedtak = new FpVedtak(LocalDateTime.now().minusYears(1), List.of(
            uttaksperiode(now(), now().plusMonths(1).minusDays(1), innvilget(ZERO)),
            uttaksperiode(now().plusMonths(1), now().plusMonths(2), avslått())), Dekningsgrad.HUNDRE, null);
        var sakFP0 = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), false, of(vedtak), null, fh(), of(), of(), MOR,
            of(), rettigheter(), false, LocalDateTime.now());

        var fpSakDto = sakFP0.tilSakDto(annenpartUbeskyttetAdresse());

        assertThat(fpSakDto.kanSøkeOmEndring()).isTrue();
    }

    private static Uttaksperiode.Resultat avslått() {
        return new Uttaksperiode.Resultat(AVSLÅTT, ANNET, uttaksperiodeAktivitet(ZERO), false);
    }

    private Rettigheter rettigheter() {
        return new Rettigheter(false, false, false);
    }

    @Test
    void kan_ikke_søke_om_endring_hvis_alle_periodene_avslått() {
        var vedtak = new FpVedtak(LocalDateTime.now().minusYears(1), List.of(
            uttaksperiode(now(), now().plusMonths(1).minusDays(1), avslått()),
            uttaksperiode(now().plusMonths(1), now().plusMonths(2), avslått())), Dekningsgrad.HUNDRE, null);
        var sakFP0 = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), false, of(vedtak), null, fh(), of(), of(), MOR,
            of(), rettigheter(), false, LocalDateTime.now());

        var fpSakDto = sakFP0.tilSakDto(annenpartUbeskyttetAdresse());

        assertThat(fpSakDto.kanSøkeOmEndring()).isFalse();
    }

    @Test
    void kan_ikke_søke_om_endring_hvis_ingen_vedtak() {
        var sakFP0 = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), false, of(), null, fh(), of(), of(), FAR, of(),
            rettigheter(), false, LocalDateTime.now());

        var fpSakDto = sakFP0.tilSakDto(annenpartUbeskyttetAdresse());

        assertThat(fpSakDto.kanSøkeOmEndring()).isFalse();
    }

    @Test
    void skal_mappe_familieHendelse() {
        var familieHendelse = fh();
        var sakFP0 = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), false, of(), null, familieHendelse, of(), of(),
            MOR, of(), rettigheter(), false, LocalDateTime.now());

        var fpSakDto = sakFP0.tilSakDto(annenpartUbeskyttetAdresse());

        assertThat(fpSakDto.familiehendelse().antallBarn()).isEqualTo(familieHendelse.antallBarn());
        assertThat(fpSakDto.familiehendelse().fødselsdato()).isEqualTo(familieHendelse.fødselsdato());
        assertThat(fpSakDto.familiehendelse().termindato()).isEqualTo(familieHendelse.termindato());
        assertThat(fpSakDto.familiehendelse().omsorgsovertakelse()).isEqualTo(familieHendelse.omsorgsovertakelse());
    }

    @Test
    void skal_mappe_aksjonspunkt_og_søknad_til_åpen_behandling() {
        var familieHendelse = fh();
        var åpenBehandling = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), false, of(), null, familieHendelse,
            Set.of(new Aksjonspunkt(Aksjonspunkt.Type.VENT_TIDLIG_SØKNAD, null, LocalDateTime.now())),
            of(new FpSøknad(SøknadStatus.MOTTATT, LocalDateTime.now(), of(), Dekningsgrad.HUNDRE, false)), MOR, of(), rettigheter(), false,
            LocalDateTime.now());
        var ikkeÅpenBehandling = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), false, of(), null, familieHendelse,
            of(new Aksjonspunkt(Aksjonspunkt.Type.VENT_TIDLIG_SØKNAD, null, LocalDateTime.now())),
            of(new FpSøknad(SøknadStatus.BEHANDLET, LocalDateTime.now(), of(), Dekningsgrad.HUNDRE, false)), MOR, of(), rettigheter(), false,
            LocalDateTime.now());


        var fpSakDto1 = åpenBehandling.tilSakDto(annenpartUbeskyttetAdresse());
        var fpSakDto2 = ikkeÅpenBehandling.tilSakDto(annenpartUbeskyttetAdresse());

        assertThat(fpSakDto1.åpenBehandling().tilstand()).isEqualTo(BehandlingTilstand.VENT_TIDLIG_SØKNAD);
        assertThat(fpSakDto2.åpenBehandling()).isNull();
    }

    @Test
    void skal_utlede_om_sak_er_mors_basert_på_bruker_rolle() {
        var mor = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), false, of(), null, fh(), of(), of(), MOR, of(), rettigheter(),
            false, LocalDateTime.now());
        var far = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), false, of(), null, fh(), of(), of(), FAR, of(), rettigheter(),
            false, LocalDateTime.now());
        var medmor = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), false, of(), null, fh(), of(), of(), MEDMOR, of(),
            rettigheter(), false, LocalDateTime.now());

        assertThat(mor.tilSakDto(annenpartUbeskyttetAdresse()).sakTilhørerMor()).isTrue();
        assertThat(far.tilSakDto(annenpartUbeskyttetAdresse()).sakTilhørerMor()).isFalse();
        assertThat(medmor.tilSakDto(annenpartUbeskyttetAdresse()).sakTilhørerMor()).isFalse();
    }

    @Test
    void skal_mappe_barn() {
        var barn1AktørId = AktørId.dummy();
        var barn2AktørId = AktørId.dummy();
        var sak = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), false, of(), null, fh(), of(), of(), MOR,
            of(barn1AktørId, barn2AktørId), rettigheter(), false, LocalDateTime.now());

        var dto = sak.tilSakDto(annenpartUbeskyttetAdresse());
        assertThat(dto.barn())
            .containsExactlyInAnyOrder(
                new Person(new Fødselsnummer(barn1AktørId.value()), null),
                new Person(new Fødselsnummer(barn2AktørId.value()), null)
            );
    }

    @Test
    void skal_mappe_rettigheter_begge_rett() {
        var uttaksperioder = List.of(uttaksperiode(now(), now(), innvilget(new Trekkdager(20))));
        var vedtak = new FpVedtak(LocalDateTime.now(), uttaksperioder, Dekningsgrad.HUNDRE, null);
        var sak = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), false, Set.of(vedtak), null, fh(), of(), of(), MOR, of(),
            new Rettigheter(false, false, false), false, LocalDateTime.now());

        var dto = sak.tilSakDto(annenpartUbeskyttetAdresse());
        assertThat(dto.rettighetType()).isEqualTo(RettighetType.BEGGE_RETT);
        assertThat(dto.morUføretrygd()).isFalse();
        assertThat(dto.harAnnenForelderTilsvarendeRettEØS()).isFalse();
    }

    @Test
    void skal_mappe_rettigheter_aleneomsorg() {
        var uttaksperioder = List.of(uttaksperiode(now(), now(),
            new Uttaksperiode.Resultat(INNVILGET, ANNET, uttaksperiodeAktivitet(new Trekkdager(20), FORELDREPENGER), false)));
        var vedtak = new FpVedtak(LocalDateTime.now(), uttaksperioder, Dekningsgrad.HUNDRE, null);
        var sak = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), false, Set.of(vedtak), null, fh(), of(), of(), MOR, of(),
            new Rettigheter(true, false, false), false, LocalDateTime.now());

        var dto = sak.tilSakDto(annenpartUbeskyttetAdresse());
        assertThat(dto.rettighetType()).isEqualTo(RettighetType.ALENEOMSORG);
        assertThat(dto.morUføretrygd()).isFalse();
        assertThat(dto.harAnnenForelderTilsvarendeRettEØS()).isFalse();
    }

    @Test
    void skal_mappe_rettigheter_enerett() {
        var uttaksperioder = List.of(uttaksperiode(now(), now(),
            new Uttaksperiode.Resultat(INNVILGET, ANNET, uttaksperiodeAktivitet(new Trekkdager(20), FORELDREPENGER), false)));
        var vedtak = new FpVedtak(LocalDateTime.now(), uttaksperioder, Dekningsgrad.HUNDRE, null);
        var sak = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), false, Set.of(vedtak), null, fh(), of(), of(), MOR, of(),
            new Rettigheter(false, true, true), false, LocalDateTime.now());

        var dto = sak.tilSakDto(annenpartUbeskyttetAdresse());
        assertThat(dto.rettighetType()).isEqualTo(RettighetType.BARE_SØKER_RETT);
        assertThat(dto.morUføretrygd()).isTrue();
        assertThat(dto.harAnnenForelderTilsvarendeRettEØS()).isTrue();
    }

    @Test
    void skal_ønsker_justering_enerett() {
        var sak = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), false, Set.of(), null, fh(), of(), of(), FAR, of(),
            rettigheter(), true, LocalDateTime.now());

        var dto = sak.tilSakDto(annenpartUbeskyttetAdresse());
        assertThat(dto.ønskerJustertUttakVedFødsel()).isTrue();
    }

    @Test
    void skal_mappe_rettigheter_begge_rett_hvis_søkt_aleneomsorg_men_uttak_er_innvilget_kvote() {
        var uttaksperioder = List.of(uttaksperiode(now(), now(),
            new Uttaksperiode.Resultat(INNVILGET, ANNET, uttaksperiodeAktivitet(new Trekkdager(20), MØDREKVOTE), false)));

        var vedtak = new FpVedtak(LocalDateTime.now(), uttaksperioder, Dekningsgrad.HUNDRE, null);
        var sak = new SakFP0(Saksnummer.dummy(), AktørId.dummy(), false, Set.of(vedtak), null, fh(), of(), of(), MOR, of(),
            new Rettigheter(true, false, false), false, LocalDateTime.now());

        var dto = sak.tilSakDto(annenpartUbeskyttetAdresse());
        assertThat(dto.rettighetType()).isEqualTo(RettighetType.BEGGE_RETT);
        assertThat(dto.morUføretrygd()).isFalse();
        assertThat(dto.harAnnenForelderTilsvarendeRettEØS()).isFalse();
    }

    private static FamilieHendelse fh() {
        return new FamilieHendelse(now(), now().plusDays(1), 1, now().plusDays(2));
    }
}
