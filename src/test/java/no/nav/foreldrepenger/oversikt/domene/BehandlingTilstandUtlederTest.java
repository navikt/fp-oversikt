package no.nav.foreldrepenger.oversikt.domene;

import static no.nav.foreldrepenger.common.innsyn.BehandlingTilstand.UNDER_BEHANDLING;
import static no.nav.foreldrepenger.common.innsyn.BehandlingTilstand.VENT_DOKUMENTASJON;
import static no.nav.foreldrepenger.common.innsyn.BehandlingTilstand.VENT_INNTEKTSMELDING;
import static no.nav.foreldrepenger.common.innsyn.BehandlingTilstand.VENT_MELDEKORT;
import static no.nav.foreldrepenger.common.innsyn.BehandlingTilstand.VENT_TIDLIG_SØKNAD;
import static no.nav.foreldrepenger.oversikt.domene.Aksjonspunkt.Status.OPPRETTET;
import static no.nav.foreldrepenger.oversikt.domene.Aksjonspunkt.Status.UTFØRT;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;

class BehandlingTilstandUtlederTest {

    @Test
    void utført_ap_gir_tilstand_under_behandling() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(aksjonspunkt("kode", UTFØRT)));
        assertThat(tilstand).isEqualTo(UNDER_BEHANDLING);
    }

    @Test
    void tidlig_søknad_opprettet_ap_gir_tilstand_tidlig_opprettet() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(aksjonspunkt(BehandlingTilstandUtleder.VENT_PGA_FOR_TIDLIG_SØKNAD, OPPRETTET)));
        assertThat(tilstand).isEqualTo(VENT_TIDLIG_SØKNAD);
    }

    @Test
    void vent_på_meldekort_opprettet_ap_gir_tilstand_vent_på_meldekort() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(aksjonspunkt(BehandlingTilstandUtleder.VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT, OPPRETTET)));
        assertThat(tilstand).isEqualTo(VENT_MELDEKORT);
    }

    @Test
    void vent_på_meldekort_manuelt_opprettet_ap_gir_tilstand_vent_på_meldekort() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(aksjonspunkt(BehandlingTilstandUtleder.MANUELT_SATT_PÅ_VENT, OPPRETTET, BehandlingTilstandUtleder.VENT_PÅ_SISTE_AAP_MELDEKORT)));
        assertThat(tilstand).isEqualTo(VENT_MELDEKORT);
    }

    @Test
    void vent_på_meldekort_manuelt_opprettet_ap_uten_ventårsak_gir_under_behandling_tilstand() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(aksjonspunkt(BehandlingTilstandUtleder.MANUELT_SATT_PÅ_VENT, OPPRETTET, null)));
        assertThat(tilstand).isEqualTo(UNDER_BEHANDLING);
    }

    @Test
    void vent_på_komplett_søknad_med_årsak_dok_gir_vent_på_dok_tilstand() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(aksjonspunkt(BehandlingTilstandUtleder.VENT_PÅ_KOMPLETT_SØKNAD, OPPRETTET, BehandlingTilstandUtleder.AVV_DOK)));
        assertThat(tilstand).isEqualTo(VENT_DOKUMENTASJON);
    }

    @Test
    void vent_på_komplett_søknad_med_årsak_null_gir_under_behandling_tilstand() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(aksjonspunkt(BehandlingTilstandUtleder.VENT_PÅ_KOMPLETT_SØKNAD, OPPRETTET, null)));
        assertThat(tilstand).isEqualTo(UNDER_BEHANDLING);
    }

    @Test
    void vent_på_komplett_søknad_med_årsak_inntektsmelding_gir_vent_på_im_tilstand() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(aksjonspunkt(BehandlingTilstandUtleder.VENT_PÅ_KOMPLETT_SØKNAD, OPPRETTET, BehandlingTilstandUtleder.VENT_OPDT_INNTEKTSMELDING)));
        assertThat(tilstand).isEqualTo(VENT_INNTEKTSMELDING);
    }

    @Test
    void vent_på_etterlyst_im_med_årsak_inntektsmelding_gir_vent_på_im_tilstand() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(aksjonspunkt(BehandlingTilstandUtleder.VENT_ETTERLYST_INNTEKTSMELDING, OPPRETTET, BehandlingTilstandUtleder.VENT_OPDT_INNTEKTSMELDING)));
        assertThat(tilstand).isEqualTo(VENT_INNTEKTSMELDING);
    }

    @Test
    void vent_på_etterlyst_im_med_årsak_null_gir_vent_på_im_tilstand() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(aksjonspunkt(BehandlingTilstandUtleder.VENT_ETTERLYST_INNTEKTSMELDING, OPPRETTET, null)));
        assertThat(tilstand).isEqualTo(VENT_INNTEKTSMELDING);
    }

    @Test
    void vent_på_inntektsmelding_manuelt_opprettet_ap_gir_tilstand_vent_på_im() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(aksjonspunkt(BehandlingTilstandUtleder.MANUELT_SATT_PÅ_VENT, OPPRETTET, BehandlingTilstandUtleder.VENT_OPDT_INNTEKTSMELDING)));
        assertThat(tilstand).isEqualTo(VENT_INNTEKTSMELDING);
    }

    private Aksjonspunkt aksjonspunkt(String kode, Aksjonspunkt.Status status) {
        return aksjonspunkt(kode, status, null);
    }

    private Aksjonspunkt aksjonspunkt(String kode, Aksjonspunkt.Status status, String venteårsak) {
        return new Aksjonspunkt(kode, status, venteårsak, LocalDateTime.now());
    }

}
