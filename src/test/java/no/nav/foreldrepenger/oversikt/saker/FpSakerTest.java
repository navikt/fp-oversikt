package no.nav.foreldrepenger.oversikt.saker;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Sak;
import no.nav.foreldrepenger.oversikt.domene.SakES0;
import no.nav.foreldrepenger.oversikt.domene.SakFP0;
import no.nav.foreldrepenger.oversikt.domene.SakSVP0;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;


class FpSakerTest {


    private static final AktørId AKTØR_ID = new AktørId("123");

    @Test
    void verifiser_at_fordeles_på_riktig_() {
        List<Sak> saker = List.of(
            new SakFP0(new Saksnummer("1"), AKTØR_ID, Set.of()),
            new SakFP0(new Saksnummer("2"), AKTØR_ID, Set.of()),
            new SakSVP0(new Saksnummer("3"), AKTØR_ID),
            new SakES0(new Saksnummer("4"), AKTØR_ID)
        );

        var sakerDto = FpSaker.tilDto(saker);

        assertThat(sakerDto.foreldrepenger()).hasSize(2);
        assertThat(sakerDto.svangerskapspenger()).hasSize(1);
        assertThat(sakerDto.engangsstønad()).hasSize(1);
    }

    @Test
    void skal_ikke_feile_ved_ingen_svp_saker() {
        List<Sak> saker = List.of(
            new SakFP0(new Saksnummer("1"), AKTØR_ID, Set.of()),
            new SakES0(new Saksnummer("3"), AKTØR_ID)
        );

        var sakerDto = FpSaker.tilDto(saker);

        assertThat(sakerDto.foreldrepenger()).hasSize(1);
        assertThat(sakerDto.svangerskapspenger()).isEmpty();
        assertThat(sakerDto.engangsstønad()).hasSize(1);
    }

    @Test
    void skal_ikke_feile_ved_bare_fp_saker() {
        List<Sak> saker = List.of(
            new SakFP0(new Saksnummer("1"), AKTØR_ID, Set.of()),
            new SakFP0(new Saksnummer("2"), AKTØR_ID, Set.of())
        );

        var sakerDto = FpSaker.tilDto(saker);

        assertThat(sakerDto.foreldrepenger()).hasSize(2);
        assertThat(sakerDto.svangerskapspenger()).isEmpty();
        assertThat(sakerDto.engangsstønad()).isEmpty();
    }

    @Test
    void skal_ikke_feile_ved_bare_svp_sak() {
        List<Sak> saker = List.of(
            new SakSVP0(new Saksnummer("1"), AKTØR_ID),
            new SakSVP0(new Saksnummer("2"), AKTØR_ID)
        );

        var sakerDto = FpSaker.tilDto(saker);

        assertThat(sakerDto.foreldrepenger()).isEmpty();
        assertThat(sakerDto.svangerskapspenger()).hasSize(2);
        assertThat(sakerDto.engangsstønad()).isEmpty();
    }

    @Test
    void skal_ikke_feile_ved_bare_es_sak() {
        List<Sak> saker = List.of(
            new SakES0(new Saksnummer("3"), AKTØR_ID)
        );

        var sakerDto = FpSaker.tilDto(saker);

        assertThat(sakerDto.foreldrepenger()).isEmpty();
        assertThat(sakerDto.svangerskapspenger()).isEmpty();
        assertThat(sakerDto.engangsstønad()).hasSize(1);
    }

    @Test
    void skal_ikke_feile_ved_ingen_saker() {
        List<Sak> saker = List.of();

        var sakerDto = FpSaker.tilDto(saker);

        assertThat(sakerDto.foreldrepenger()).isEmpty();
        assertThat(sakerDto.svangerskapspenger()).isEmpty();
        assertThat(sakerDto.engangsstønad()).isEmpty();
    }

}
