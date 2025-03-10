package no.nav.foreldrepenger.oversikt.saker;

import static java.time.LocalDateTime.now;
import static no.nav.foreldrepenger.oversikt.stub.DummyPersonOppslagSystemTest.annenpartUbeskyttetAdresse;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.Sak;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.SøknadStatus;
import no.nav.foreldrepenger.oversikt.domene.es.EsSøknad;
import no.nav.foreldrepenger.oversikt.domene.es.EsVedtak;
import no.nav.foreldrepenger.oversikt.domene.es.SakES0;
import no.nav.foreldrepenger.oversikt.domene.fp.BrukerRolle;
import no.nav.foreldrepenger.oversikt.domene.fp.Dekningsgrad;
import no.nav.foreldrepenger.oversikt.domene.fp.FpSøknad;
import no.nav.foreldrepenger.oversikt.domene.fp.FpSøknadsperiode;
import no.nav.foreldrepenger.oversikt.domene.fp.FpVedtak;
import no.nav.foreldrepenger.oversikt.domene.fp.Konto;
import no.nav.foreldrepenger.oversikt.domene.fp.Rettigheter;
import no.nav.foreldrepenger.oversikt.domene.fp.SakFP0;
import no.nav.foreldrepenger.oversikt.domene.svp.Aktivitet;
import no.nav.foreldrepenger.oversikt.domene.svp.SakSVP0;
import no.nav.foreldrepenger.oversikt.domene.svp.SvpSøknad;
import no.nav.foreldrepenger.oversikt.domene.svp.SvpVedtak;
import no.nav.foreldrepenger.oversikt.domene.svp.Tilrettelegging;


class SakerDtoMapperTest {


    private static final AktørId AKTØR_ID = AktørId.dummy();

    @Test
    void verifiser_at_fordeles_på_riktig_() {
        List<Sak> saker = List.of(fpSak(), fpSak(), svpSak(), esSak());

        var sakerDto = SakerDtoMapper.tilDto(saker, annenpartUbeskyttetAdresse());

        assertThat(sakerDto.foreldrepenger()).hasSize(2);
        assertThat(sakerDto.svangerskapspenger()).hasSize(1);
        assertThat(sakerDto.engangsstønad()).hasSize(1);
    }

    @Test
    void skal_ikke_feile_ved_ingen_svp_saker() {
        List<Sak> saker = List.of(fpSak(), esSak());

        var sakerDto = SakerDtoMapper.tilDto(saker, annenpartUbeskyttetAdresse());

        assertThat(sakerDto.foreldrepenger()).hasSize(1);
        assertThat(sakerDto.svangerskapspenger()).isEmpty();
        assertThat(sakerDto.engangsstønad()).hasSize(1);
    }

    @Test
    void skal_ikke_feile_ved_bare_fp_saker() {
        List<Sak> saker = List.of(fpSak(), fpSak());

        var sakerDto = SakerDtoMapper.tilDto(saker, annenpartUbeskyttetAdresse());

        assertThat(sakerDto.foreldrepenger()).hasSize(2);
        assertThat(sakerDto.svangerskapspenger()).isEmpty();
        assertThat(sakerDto.engangsstønad()).isEmpty();
    }

    @Test
    void skal_ikke_feile_ved_bare_svp_sak() {
        List<Sak> saker = List.of(svpSak(), svpSak());

        var sakerDto = SakerDtoMapper.tilDto(saker, annenpartUbeskyttetAdresse());

        assertThat(sakerDto.foreldrepenger()).isEmpty();
        assertThat(sakerDto.svangerskapspenger()).hasSize(2);
        assertThat(sakerDto.engangsstønad()).isEmpty();
    }

    @Test
    void skal_ikke_feile_ved_bare_es_sak() {
        List<Sak> saker = List.of(esSak());

        var sakerDto = SakerDtoMapper.tilDto(saker, annenpartUbeskyttetAdresse());

        assertThat(sakerDto.foreldrepenger()).isEmpty();
        assertThat(sakerDto.svangerskapspenger()).isEmpty();
        assertThat(sakerDto.engangsstønad()).hasSize(1);
    }

    @Test
    void skal_ikke_feile_ved_ingen_saker() {
        List<Sak> saker = List.of();

        var sakerDto = SakerDtoMapper.tilDto(saker, annenpartUbeskyttetAdresse());

        assertThat(sakerDto.foreldrepenger()).isEmpty();
        assertThat(sakerDto.svangerskapspenger()).isEmpty();
        assertThat(sakerDto.engangsstønad()).isEmpty();
    }

    private static SakES0 esSak() {
        return esSak(AKTØR_ID);
    }

    private static SakSVP0 svpSak() {
        return svpSak(AKTØR_ID);
    }

    private static SakFP0 fpSak() {
        return fpSak(AKTØR_ID);
    }

    static SakES0 esSak(AktørId aktørId) {
        return new SakES0(Saksnummer.dummy(), aktørId, true, fh(), Set.of(), Set.of(new EsSøknad(SøknadStatus.MOTTATT, now())),
            Set.of(new EsVedtak(LocalDateTime.now())), now());
    }

    static SakSVP0 svpSak(AktørId aktørId) {
        return new SakSVP0(Saksnummer.dummy(), aktørId, false, fh(), Set.of(), Set.of(new SvpSøknad(SøknadStatus.MOTTATT, now(), Set.of(new Tilrettelegging(new Aktivitet(
            Aktivitet.Type.FRILANS, null, null, null), null, null, null, Set.of(), Set.of())))),
            Set.of(new SvpVedtak(LocalDateTime.now(), Set.of(), null)), now());
    }

    static SakFP0 fpSak(AktørId aktørId) {
        var dekningsgrad = Dekningsgrad.HUNDRE;
        var vedtak = new FpVedtak(LocalDateTime.now(), List.of(), dekningsgrad);
        return new SakFP0(Saksnummer.dummy(), aktørId, true, Set.of(vedtak), AktørId.dummy(), fh(), Set.of(),
            Set.of(new FpSøknad(SøknadStatus.MOTTATT, now(), Set.of(new FpSøknadsperiode(LocalDate.now(), LocalDate.now(), Konto.MØDREKVOTE,
                null, null, null, null, null, false, null)), dekningsgrad)), BrukerRolle.MOR,
            Set.of(), new Rettigheter(false, false, false), false, now());
    }

    static SakFP0 fpSakUtenSøknad(AktørId aktørId) {
        return new SakFP0(Saksnummer.dummy(), aktørId, true, Set.of(), AktørId.dummy(), fh(), Set.of(), Set.of(), BrukerRolle.MOR,
            Set.of(), new Rettigheter(false, false, false), false, now());
    }

    static FamilieHendelse fh() {
        return new FamilieHendelse(null, LocalDate.now(), 1, null);
    }
}
