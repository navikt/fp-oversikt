package no.nav.foreldrepenger.oversikt.domene;

import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.common.innsyn.BehandlingTilstand;
import no.nav.foreldrepenger.konfig.Environment;

public final class BehandlingTilstandUtleder {

    private static final Logger LOG = LoggerFactory.getLogger(BehandlingTilstandUtleder.class);
    private static final int SEKUNDER_VENTETID_PÅ_PROSESSERING_AV_SØKNAD = 15;
    private static final Environment ENV = Environment.current();

    private BehandlingTilstandUtleder() {
    }

    public static BehandlingTilstand utled(Set<Aksjonspunkt> ap, LocalDateTime søknadMottattTidspunkt) {

        var aksjonspunkt = safeStream(ap).collect(Collectors.toSet());
        var tilstand = utledGittOpprettetAksjonspunkt(aksjonspunkt, søknadMottattTidspunkt);
        LOG.info("Utledet behandlingtilstand {} for aksjonspunkter {} søknad mottatt {}", tilstand, ap, søknadMottattTidspunkt);
        if (tilstand == BehandlingTilstand.PROSESSERER && ENV.isProd()) {
            return BehandlingTilstand.UNDER_BEHANDLING; // TODO TFP-5621 Til frontend er over på ny status
        }
        return tilstand;
    }

    private static BehandlingTilstand utledGittOpprettetAksjonspunkt(Set<Aksjonspunkt> opprettetAksjonspunkt, LocalDateTime søknadMottattTidspunkt) {
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
        // TODO utvid med tilstand VENT_UTLAND_TRYGD basert på venteårsak UTLAND_TRYGD
        if (opprettetAksjonspunkt.isEmpty()) {
            if (søknadMottattTidspunkt.plusSeconds(SEKUNDER_VENTETID_PÅ_PROSESSERING_AV_SØKNAD).isAfter(LocalDateTime.now())) {
                return BehandlingTilstand.PROSESSERER;
            }
            LOG.info("Ingen aksjonspunkt og søknad mottatt for over {} sekunder siden, returnerer {}", SEKUNDER_VENTETID_PÅ_PROSESSERING_AV_SØKNAD,
                BehandlingTilstand.UNDER_BEHANDLING);
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
