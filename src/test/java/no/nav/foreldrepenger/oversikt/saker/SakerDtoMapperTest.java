package no.nav.foreldrepenger.oversikt.saker;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
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

    static FødselsnummerOppslag fnrOppslag() {
        return AktørId::value;
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
        return new SakES0(Saksnummer.dummy(), aktørId, SakStatus.AVSLUTTET, fh(), Set.of(), Set.of(new EsSøknad(SøknadStatus.MOTTATT, now())),
            now());
    }

    static SakSVP0 svpSak(AktørId aktørId) {
        return new SakSVP0(Saksnummer.dummy(), aktørId, SakStatus.UNDER_BEHANDLING, fh(), Set.of(), Set.of(new SvpSøknad(SøknadStatus.MOTTATT, now())),
            now());
    }

    static SakFP0 fpSak(AktørId aktørId) {
        return new SakFP0(Saksnummer.dummy(), aktørId, SakStatus.AVSLUTTET, Set.of(), AktørId.dummy(), fh(), Set.of(),
            Set.of(new FpSøknad(SøknadStatus.MOTTATT, now(), Set.of(), Dekningsgrad.HUNDRE)), BrukerRolle.MOR,
            Set.of(), new Rettigheter(false, false, false), false, now());
    }

    static SakFP0 fpSakUtenSøknad(AktørId aktørId) {
        return new SakFP0(Saksnummer.dummy(), aktørId, SakStatus.AVSLUTTET, Set.of(), AktørId.dummy(), fh(), Set.of(), Set.of(), BrukerRolle.MOR,
            Set.of(), new Rettigheter(false, false, false), false, now());
    }

    static FamilieHendelse fh() {
        return new FamilieHendelse(null, LocalDate.now(), 1, null);
    }
}
