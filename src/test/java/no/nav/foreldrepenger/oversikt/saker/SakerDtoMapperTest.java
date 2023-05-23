package no.nav.foreldrepenger.oversikt.saker;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.BrukerRolle;
import no.nav.foreldrepenger.oversikt.domene.Dekningsgrad;
import no.nav.foreldrepenger.oversikt.domene.EsSøknad;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.FpSøknad;
import no.nav.foreldrepenger.oversikt.domene.Rettigheter;
import no.nav.foreldrepenger.oversikt.domene.Sak;
import no.nav.foreldrepenger.oversikt.domene.SakES0;
import no.nav.foreldrepenger.oversikt.domene.SakFP0;
import no.nav.foreldrepenger.oversikt.domene.SakSVP0;
import no.nav.foreldrepenger.oversikt.domene.SakStatus;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.SvpSøknad;
import no.nav.foreldrepenger.oversikt.domene.SøknadStatus;


class SakerDtoMapperTest {


    private static final AktørId AKTØR_ID = AktørId.dummy();

    @Test
    void verifiser_at_fordeles_på_riktig_() {
        List<Sak> saker = List.of(fpSak(), fpSak(), svpSak(), esSak());

        var sakerDto = SakerDtoMapper.tilDto(saker, fnrOppslag());

        assertThat(sakerDto.foreldrepenger()).hasSize(2);
        assertThat(sakerDto.svangerskapspenger()).hasSize(1);
        assertThat(sakerDto.engangsstønad()).hasSize(1);
    }

    @Test
    void skal_ikke_feile_ved_ingen_svp_saker() {
        List<Sak> saker = List.of(fpSak(), esSak());

        var sakerDto = SakerDtoMapper.tilDto(saker, fnrOppslag());

        assertThat(sakerDto.foreldrepenger()).hasSize(1);
        assertThat(sakerDto.svangerskapspenger()).isEmpty();
        assertThat(sakerDto.engangsstønad()).hasSize(1);
    }

    @Test
    void skal_ikke_feile_ved_bare_fp_saker() {
        List<Sak> saker = List.of(fpSak(), fpSak());

        var sakerDto = SakerDtoMapper.tilDto(saker, fnrOppslag());

        assertThat(sakerDto.foreldrepenger()).hasSize(2);
        assertThat(sakerDto.svangerskapspenger()).isEmpty();
        assertThat(sakerDto.engangsstønad()).isEmpty();
    }

    @Test
    void skal_ikke_feile_ved_bare_svp_sak() {
        List<Sak> saker = List.of(svpSak(), svpSak());

        var sakerDto = SakerDtoMapper.tilDto(saker, fnrOppslag());

        assertThat(sakerDto.foreldrepenger()).isEmpty();
        assertThat(sakerDto.svangerskapspenger()).hasSize(2);
        assertThat(sakerDto.engangsstønad()).isEmpty();
    }

    @Test
    void skal_ikke_feile_ved_bare_es_sak() {
        List<Sak> saker = List.of(esSak());

        var sakerDto = SakerDtoMapper.tilDto(saker, fnrOppslag());

        assertThat(sakerDto.foreldrepenger()).isEmpty();
        assertThat(sakerDto.svangerskapspenger()).isEmpty();
        assertThat(sakerDto.engangsstønad()).hasSize(1);
    }

    @Test
    void skal_ikke_feile_ved_ingen_saker() {
        List<Sak> saker = List.of();

        var sakerDto = SakerDtoMapper.tilDto(saker, fnrOppslag());

        assertThat(sakerDto.foreldrepenger()).isEmpty();
        assertThat(sakerDto.svangerskapspenger()).isEmpty();
        assertThat(sakerDto.engangsstønad()).isEmpty();
    }

    @Test
    void skal_ikke_returne_saker_uten_søknad() {
        List<Sak> saker = List.of(fpSak(), fpSak(), fpSakUtenSøknad(), svpSak(), esSak());

        var sakerDto = SakerDtoMapper.tilDto(saker, fnrOppslag());

        assertThat(sakerDto.foreldrepenger()).hasSize(2);
        assertThat(sakerDto.svangerskapspenger()).hasSize(1);
        assertThat(sakerDto.engangsstønad()).hasSize(1);
    }

    private static FødselsnummerOppslag fnrOppslag() {
        return AktørId::value;
    }

    private static SakES0 esSak() {
        return new SakES0(Saksnummer.dummy(), AKTØR_ID, SakStatus.AVSLUTTET, fh(), Set.of(), Set.of(new EsSøknad(SøknadStatus.MOTTATT, LocalDateTime.now())));
    }

    private static SakSVP0 svpSak() {
        return new SakSVP0(Saksnummer.dummy(), AKTØR_ID, SakStatus.UNDER_BEHANDLING, fh(), Set.of(), Set.of(new SvpSøknad(SøknadStatus.MOTTATT, LocalDateTime.now())));
    }

    private static SakFP0 fpSak() {
        return new SakFP0(Saksnummer.dummy(), AKTØR_ID, SakStatus.AVSLUTTET, Set.of(), AktørId.dummy(), fh(), Set.of(),
            Set.of(new FpSøknad(SøknadStatus.MOTTATT, LocalDateTime.now(), Set.of(), Dekningsgrad.HUNDRE)), BrukerRolle.MOR,
            Set.of(), new Rettigheter(false, false, false), false);
    }

    private static SakFP0 fpSakUtenSøknad() {
        return new SakFP0(Saksnummer.dummy(), AKTØR_ID, SakStatus.AVSLUTTET, Set.of(), AktørId.dummy(), fh(), Set.of(), Set.of(), BrukerRolle.MOR,
            Set.of(), new Rettigheter(false, false, false), false);
    }

    private static FamilieHendelse fh() {
        return new FamilieHendelse(null, LocalDate.now(), 1, null);
    }
}
