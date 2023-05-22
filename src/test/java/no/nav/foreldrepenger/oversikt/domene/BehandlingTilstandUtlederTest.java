package no.nav.foreldrepenger.oversikt.domene;

import static no.nav.foreldrepenger.common.innsyn.BehandlingTilstand.UNDER_BEHANDLING;
import static no.nav.foreldrepenger.common.innsyn.BehandlingTilstand.VENT_DOKUMENTASJON;
import static no.nav.foreldrepenger.common.innsyn.BehandlingTilstand.VENT_INNTEKTSMELDING;
import static no.nav.foreldrepenger.common.innsyn.BehandlingTilstand.VENT_MELDEKORT;
import static no.nav.foreldrepenger.common.innsyn.BehandlingTilstand.VENT_TIDLIG_SØKNAD;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;

class BehandlingTilstandUtlederTest {

    @Test
    void ingen_ap_gir_under_behandling() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of());
        assertThat(tilstand).isEqualTo(UNDER_BEHANDLING);
    }

    @Test
    void tidlig_søknad_opprettet_ap_gir_tilstand_tidlig_opprettet() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(aksjonspunkt(Aksjonspunkt.Type.VENT_TIDLIG_SØKNAD)));
        assertThat(tilstand).isEqualTo(VENT_TIDLIG_SØKNAD);
    }

    @Test
    void vent_på_meldekort_opprettet_ap_gir_tilstand_vent_på_meldekort() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(aksjonspunkt(Aksjonspunkt.Type.VENT_SISTE_AAP_ELLER_DP_MELDEKORT)));
        assertThat(tilstand).isEqualTo(VENT_MELDEKORT);
    }

    @Test
    void vent_på_meldekort_manuelt_opprettet_ap_gir_tilstand_vent_på_meldekort() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(aksjonspunkt(Aksjonspunkt.Type.VENT_MANUELT_SATT,
            Aksjonspunkt.Venteårsak.SISTE_AAP_ELLER_DP_MELDEKORT)));
        assertThat(tilstand).isEqualTo(VENT_MELDEKORT);
    }

    @Test
    void vent_på_meldekort_manuelt_opprettet_ap_uten_ventårsak_gir_under_behandling_tilstand() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(aksjonspunkt(Aksjonspunkt.Type.VENT_MANUELT_SATT, null)));
        assertThat(tilstand).isEqualTo(UNDER_BEHANDLING);
    }

    @Test
    void vent_på_komplett_søknad_med_årsak_dok_gir_vent_på_dok_tilstand() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(aksjonspunkt(Aksjonspunkt.Type.VENT_KOMPLETT_SØKNAD,
            Aksjonspunkt.Venteårsak.AVVENT_DOKUMTANSJON)));
        assertThat(tilstand).isEqualTo(VENT_DOKUMENTASJON);
    }

    @Test
    void vent_på_komplett_søknad_med_årsak_null_gir_under_behandling_tilstand() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(aksjonspunkt(Aksjonspunkt.Type.VENT_KOMPLETT_SØKNAD, null)));
        assertThat(tilstand).isEqualTo(UNDER_BEHANDLING);
    }

    @Test
    void vent_på_komplett_søknad_med_årsak_inntektsmelding_gir_vent_på_im_tilstand() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(aksjonspunkt(Aksjonspunkt.Type.VENT_KOMPLETT_SØKNAD,
            Aksjonspunkt.Venteårsak.MANGLENDE_INNTEKTSMELDING)));
        assertThat(tilstand).isEqualTo(VENT_INNTEKTSMELDING);
    }

    @Test
    void vent_på_etterlyst_im_med_årsak_inntektsmelding_gir_vent_på_im_tilstand() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(aksjonspunkt(Aksjonspunkt.Type.VENT_ETTERLYST_INNTEKTSMELDING,
            Aksjonspunkt.Venteårsak.MANGLENDE_INNTEKTSMELDING)));
        assertThat(tilstand).isEqualTo(VENT_INNTEKTSMELDING);
    }

    @Test
    void vent_på_etterlyst_im_med_årsak_null_gir_vent_på_im_tilstand() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(aksjonspunkt(Aksjonspunkt.Type.VENT_ETTERLYST_INNTEKTSMELDING, null)));
        assertThat(tilstand).isEqualTo(VENT_INNTEKTSMELDING);
    }

    @Test
    void vent_på_inntektsmelding_manuelt_opprettet_ap_gir_tilstand_vent_på_im() {
        var tilstand = BehandlingTilstandUtleder.utled(Set.of(aksjonspunkt(Aksjonspunkt.Type.VENT_MANUELT_SATT,
            Aksjonspunkt.Venteårsak.MANGLENDE_INNTEKTSMELDING)));
        assertThat(tilstand).isEqualTo(VENT_INNTEKTSMELDING);
    }

    private Aksjonspunkt aksjonspunkt(Aksjonspunkt.Type type) {
        return aksjonspunkt(type, null);
    }

    private Aksjonspunkt aksjonspunkt(Aksjonspunkt.Type type, Aksjonspunkt.Venteårsak venteårsak) {
        return new Aksjonspunkt(type, venteårsak, LocalDateTime.now());
    }

}
