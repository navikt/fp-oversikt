package no.nav.foreldrepenger.oversikt.domene;

import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.common.innsyn.BehandlingTilstand;

final class BehandlingTilstandUtleder {

    private static final Logger LOG = LoggerFactory.getLogger(BehandlingTilstandUtleder.class);

    private BehandlingTilstandUtleder() {
    }

    static BehandlingTilstand utled(Set<Aksjonspunkt> ap) {

        var aksjonspunkt = safeStream(ap).collect(Collectors.toSet());
        var tilstand = utledGittOpprettetAksjonspunkt(aksjonspunkt);
        LOG.info("Utledet behandlingtilstand {} for aksjonspunkter {}", tilstand, ap);
        return tilstand;
    }

    private static BehandlingTilstand utledGittOpprettetAksjonspunkt(Set<Aksjonspunkt> opprettetAksjonspunkt) {
        if (contains(opprettetAksjonspunkt, Aksjonspunkt.Type.VENT_TIDLIG_SØKNAD)) {
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
        return contains(opprettetAksjonspunkt, Aksjonspunkt.Type.VENT_KOMPLETT_SØKNAD, Aksjonspunkt.Venteårsak.AVVENT_DOKUMTANSJON);
    }

    private static boolean venterPåMeldekort(Set<Aksjonspunkt> opprettetAksjonspunkt) {
        return contains(opprettetAksjonspunkt, Aksjonspunkt.Type.VENT_SISTE_AAP_ELLER_DP_MELDEKORT)
            || contains(opprettetAksjonspunkt, Aksjonspunkt.Type.VENT_MANUELT_SATT, Aksjonspunkt.Venteårsak.SISTE_AAP_ELLER_DP_MELDEKORT);
    }

    private static boolean venterPåInntektsmelding(Set<Aksjonspunkt> opprettetAksjonspunkt) {
        return contains(opprettetAksjonspunkt, Aksjonspunkt.Type.VENT_KOMPLETT_SØKNAD, Aksjonspunkt.Venteårsak.MANGLENDE_INNTEKTSMELDING)
            || contains(opprettetAksjonspunkt, Aksjonspunkt.Type.VENT_ETTERLYST_INNTEKTSMELDING)
            || contains(opprettetAksjonspunkt, Aksjonspunkt.Type.VENT_MANUELT_SATT, Aksjonspunkt.Venteårsak.MANGLENDE_INNTEKTSMELDING);
    }

    private static boolean contains(Set<Aksjonspunkt> opprettetAksjonspunkt, Aksjonspunkt.Type type) {
        return contains(opprettetAksjonspunkt, type, null);
    }

    private static boolean contains(Set<Aksjonspunkt> opprettetAksjonspunkt,
                                    Aksjonspunkt.Type type,
                                    Aksjonspunkt.Venteårsak venteårsak) {
        return opprettetAksjonspunkt.stream()
                .anyMatch(a -> Objects.equals(a.type(), type) && (venteårsak == null || Objects.equals(a.venteårsak(), venteårsak)));
    }
}
