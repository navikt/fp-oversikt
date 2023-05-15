package no.nav.foreldrepenger.oversikt.domene;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.common.innsyn.BehandlingTilstand;
import no.nav.foreldrepenger.common.util.StreamUtil;

final class BehandlingTilstandUtleder {

    static final String AVV_DOK = "AVV_DOK";
    static final String VENT_PÅ_SISTE_AAP_MELDEKORT = "VENT_PÅ_SISTE_AAP_MELDEKORT";
    static final String VENT_OPDT_INNTEKTSMELDING = "VENT_OPDT_INNTEKTSMELDING";

    static final String MANUELT_SATT_PÅ_VENT = "7001";
    static final String VENT_PÅ_KOMPLETT_SØKNAD = "7003";
    static final String VENT_PGA_FOR_TIDLIG_SØKNAD = "7008";
    static final String VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT = "7020";
    static final String VENT_ETTERLYST_INNTEKTSMELDING = "7030";

    private static final Logger LOG = LoggerFactory.getLogger(BehandlingTilstandUtleder.class);

    private BehandlingTilstandUtleder() {
    }

    static BehandlingTilstand utled(Set<Aksjonspunkt> aksjonspunkter) {
        var opprettetAksjonspunkt = StreamUtil.safeStream(aksjonspunkter)
                .filter(ap -> ap.status() == Aksjonspunkt.Status.OPPRETTET)
                .collect(Collectors.toSet());

        var tilstand = utledGittOpprettetAksjonspunkt(opprettetAksjonspunkt);
        LOG.info("Utledet behandlingtilstand {} for aksjonspunkter {}", tilstand, aksjonspunkter);
        return tilstand;
    }

    private static BehandlingTilstand utledGittOpprettetAksjonspunkt(Set<Aksjonspunkt> opprettetAksjonspunkt) {
        if (contains(opprettetAksjonspunkt, VENT_PGA_FOR_TIDLIG_SØKNAD)) {
            return BehandlingTilstand.VENT_TIDLIG_SØKNAD;
        }
        if (venterPåMeldekort(opprettetAksjonspunkt)) {
            return BehandlingTilstand.VENT_MELDEKORT;
        }
        if (venterPåDokumentasjonFraBruker(opprettetAksjonspunkt)) {
            return BehandlingTilstand.VENT_DOKUMENTASJON;
        }
        if (venterPåInntektsmelding(opprettetAksjonspunkt)) {
            return BehandlingTilstand.VENT_INNTEKTSMELDING;
        }
        return BehandlingTilstand.UNDER_BEHANDLING;
    }

    private static boolean venterPåDokumentasjonFraBruker(Set<Aksjonspunkt> opprettetAksjonspunkt) {
        return contains(opprettetAksjonspunkt, VENT_PÅ_KOMPLETT_SØKNAD, AVV_DOK);
    }

    private static boolean venterPåMeldekort(Set<Aksjonspunkt> opprettetAksjonspunkt) {
        return contains(opprettetAksjonspunkt, VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT)
            || contains(opprettetAksjonspunkt, MANUELT_SATT_PÅ_VENT, VENT_PÅ_SISTE_AAP_MELDEKORT);
    }

    private static boolean venterPåInntektsmelding(Set<Aksjonspunkt> opprettetAksjonspunkt) {
        return contains(opprettetAksjonspunkt, VENT_PÅ_KOMPLETT_SØKNAD, VENT_OPDT_INNTEKTSMELDING)
            || contains(opprettetAksjonspunkt, VENT_ETTERLYST_INNTEKTSMELDING)
            || contains(opprettetAksjonspunkt, MANUELT_SATT_PÅ_VENT, VENT_OPDT_INNTEKTSMELDING);
    }

    private static boolean contains(Set<Aksjonspunkt> opprettetAksjonspunkt, String kode) {
        return contains(opprettetAksjonspunkt, kode, null);
    }

    private static boolean contains(Set<Aksjonspunkt> opprettetAksjonspunkt,
                                    String kode,
                                    String venteårsak) {
        return opprettetAksjonspunkt.stream()
                .anyMatch(a -> Objects.equals(a.kode(), kode) && (venteårsak == null || Objects.equals(a.venteÅrsak(), venteårsak)));
    }
}
