package no.nav.foreldrepenger.oversikt.domene;

import static java.time.LocalDateTime.now;
import static no.nav.foreldrepenger.common.innsyn.BehandlingTilstand.PROSESSERER;
import static no.nav.foreldrepenger.common.innsyn.BehandlingTilstand.UNDER_BEHANDLING;
import static no.nav.foreldrepenger.common.innsyn.BehandlingTilstand.VENT_DOKUMENTASJON;
import static no.nav.foreldrepenger.common.innsyn.BehandlingTilstand.VENT_INNTEKTSMELDING;
import static no.nav.foreldrepenger.common.innsyn.BehandlingTilstand.VENT_MELDEKORT;
import static no.nav.foreldrepenger.common.innsyn.BehandlingTilstand.VENT_TIDLIG_SØKNAD;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

class BehandlingTilstandUtlederTest {

    @Test
    void ingen_ap_gir_under_behandling() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(), now().minusDays(1));
        assertThat(tilstand).isEqualTo(UNDER_BEHANDLING);
    }

    @Test
    void tidlig_søknad_opprettet_ap_gir_tilstand_tidlig_opprettet() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(aksjonspunkt(Aksjonspunkt.Type.VENT_TIDLIG_SØKNAD)), now().minusDays(1));
        assertThat(tilstand).isEqualTo(VENT_TIDLIG_SØKNAD);
    }

    @Test
    void vent_på_meldekort_opprettet_ap_gir_tilstand_vent_på_meldekort() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(aksjonspunkt(Aksjonspunkt.Type.VENT_SISTE_AAP_ELLER_DP_MELDEKORT)), now().minusDays(1));
        assertThat(tilstand).isEqualTo(VENT_MELDEKORT);
    }

    @Test
    void vent_på_meldekort_manuelt_opprettet_ap_gir_tilstand_vent_på_meldekort() {
        var tilstand = BehandlingTilstandUtleder.utled(
            Set.of(aksjonspunkt(Aksjonspunkt.Type.VENT_MANUELT_SATT, Aksjonspunkt.Venteårsak.SISTE_AAP_ELLER_DP_MELDEKORT)), now().minusDays(1));
        assertThat(tilstand).isEqualTo(VENT_MELDEKORT);
    }

    @Test
    void vent_på_meldekort_manuelt_opprettet_ap_uten_ventårsak_gir_under_behandling_tilstand() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(aksjonspunkt(Aksjonspunkt.Type.VENT_MANUELT_SATT, null)), now().minusDays(1));
        assertThat(tilstand).isEqualTo(UNDER_BEHANDLING);
    }

    @Test
    void vent_på_komplett_søknad_med_årsak_dok_gir_vent_på_dok_tilstand() {
        var tilstand = BehandlingTilstandUtleder.utled(
            Set.of(aksjonspunkt(Aksjonspunkt.Type.VENT_KOMPLETT_SØKNAD, Aksjonspunkt.Venteårsak.AVVENT_DOKUMTANSJON)), now().minusDays(1));
        assertThat(tilstand).isEqualTo(VENT_DOKUMENTASJON);
    }

    @Test
    void vent_på_komplett_søknad_med_årsak_null_gir_under_behandling_tilstand() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(aksjonspunkt(Aksjonspunkt.Type.VENT_KOMPLETT_SØKNAD, null)), now().minusDays(1));
        assertThat(tilstand).isEqualTo(UNDER_BEHANDLING);
    }

    @Test
    void vent_på_komplett_søknad_med_årsak_inntektsmelding_gir_vent_på_im_tilstand() {
        var tilstand = BehandlingTilstandUtleder.utled(
            Set.of(aksjonspunkt(Aksjonspunkt.Type.VENT_KOMPLETT_SØKNAD, Aksjonspunkt.Venteårsak.MANGLENDE_INNTEKTSMELDING)), now().minusDays(1));
        assertThat(tilstand).isEqualTo(VENT_INNTEKTSMELDING);
    }

    @Test
    void vent_på_etterlyst_im_med_årsak_inntektsmelding_gir_vent_på_im_tilstand() {
        var tilstand = BehandlingTilstandUtleder.utled(
            Set.of(aksjonspunkt(Aksjonspunkt.Type.VENT_ETTERLYST_INNTEKTSMELDING, Aksjonspunkt.Venteårsak.MANGLENDE_INNTEKTSMELDING)),
            now().minusDays(1));
        assertThat(tilstand).isEqualTo(VENT_INNTEKTSMELDING);
    }

    @Test
    void vent_på_etterlyst_im_med_årsak_null_gir_vent_på_im_tilstand() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(aksjonspunkt(Aksjonspunkt.Type.VENT_ETTERLYST_INNTEKTSMELDING, null)),
            now().minusDays(1));
        assertThat(tilstand).isEqualTo(VENT_INNTEKTSMELDING);
    }

    @Test
    void vent_på_inntektsmelding_manuelt_opprettet_ap_gir_tilstand_vent_på_im() {
        var tilstand = BehandlingTilstandUtleder.utled(
            Set.of(aksjonspunkt(Aksjonspunkt.Type.VENT_MANUELT_SATT, Aksjonspunkt.Venteårsak.MANGLENDE_INNTEKTSMELDING)), now().minusDays(1));
        assertThat(tilstand).isEqualTo(VENT_INNTEKTSMELDING);
    }

    @Test
    void annet_aksjonspunkt_gir_under_behandling_tilstand_selv_om_søknad_mottatt_nylig() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(aksjonspunkt(Aksjonspunkt.Type.ANNET)), now().minusSeconds(1));
        assertThat(tilstand).isEqualTo(UNDER_BEHANDLING);
    }

    @Test
    void ingen_aksjonspunkt_og_søknad_mottatt_nylig_gir_prosesserer_tilstand() {
        var søknadMottattTidspunkt = now().minusSeconds(10);
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(), søknadMottattTidspunkt);
        assertThat(tilstand).isEqualTo(PROSESSERER);
    }

    @Test
    void ingen_aksjonspunkt_og_søknad_mottatt_for_lenge_siden_gir_under_behandling_tilstand() {
        var søknadMottattTidspunkt = now().minusSeconds(20);
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(), søknadMottattTidspunkt);
        assertThat(tilstand).isEqualTo(UNDER_BEHANDLING);
    }

    @Test
    void ingen_aksjonspunkt_og_søknad_mottatt_akkurat_15_sekunder_siden_gir_under_behandling_tilstand() {
        var søknadMottattTidspunkt = now().minusSeconds(15);
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(), søknadMottattTidspunkt);
        assertThat(tilstand).isEqualTo(UNDER_BEHANDLING);
    }

    private Aksjonspunkt aksjonspunkt(Aksjonspunkt.Type type) {
        return aksjonspunkt(type, null);
    }

    private Aksjonspunkt aksjonspunkt(Aksjonspunkt.Type type, Aksjonspunkt.Venteårsak venteårsak) {
        return new Aksjonspunkt(type, venteårsak, now());
    }

}
