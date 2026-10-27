package no.nav.foreldrepenger.oversikt.soknadsplan;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record Søknadsplan(Instant mottattTidspunkt, Søknadsplan.Rolle rolle, List<SøknadsplanPeriode> perioder) {

    public Søknadsplan {
        Objects.requireNonNull(mottattTidspunkt, "mottattTidspunkt kan ikke være null");
        Objects.requireNonNull(rolle, "rolle kan ikke være null");
        perioder = List.copyOf(Objects.requireNonNull(perioder, "perioder kan ikke være null"));
        if (perioder.isEmpty()) {
            throw new IllegalArgumentException("perioder kan ikke være tom");
        }
    }

    @Override
    public String toString() {
        return "Søknadsplan[redacted]";
    }

    public enum Rolle {
        MOR,
        FAR_MEDMOR
    }
}
