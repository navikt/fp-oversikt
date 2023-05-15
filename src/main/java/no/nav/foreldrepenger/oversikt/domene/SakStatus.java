package no.nav.foreldrepenger.oversikt.domene;

import java.util.Objects;

public enum SakStatus {
    OPPRETTET,
    UNDER_BEHANDLING,
    LØPENDE,
    AVSLUTTET,
    ;

    public static boolean avsluttet(SakStatus status) {
        return Objects.equals(status, AVSLUTTET);
    }
}
