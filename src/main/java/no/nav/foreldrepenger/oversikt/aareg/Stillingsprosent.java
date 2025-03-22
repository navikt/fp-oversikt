package no.nav.foreldrepenger.oversikt.aareg;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Stillingsprosent slik det er oppgitt i arbeidsavtalen
 */
public record Stillingsprosent(BigDecimal verdi) implements Comparable<Stillingsprosent> {

    private static final RoundingMode AVRUNDINGSMODUS = RoundingMode.HALF_EVEN;

    private static final BigDecimal ARBEID_MAX_VERDI = BigDecimal.valueOf(109.99d); // Bør være 100 men legger på litt slack (10,75 vs 107,5)

    public Stillingsprosent {
        Objects.requireNonNull(verdi, "Stillingsprosent kan ikke være null");
        if (BigDecimal.ZERO.compareTo(verdi) > 0) {
            throw new IllegalArgumentException("Prosent må være >= 0");
        }
    }

    public static Stillingsprosent arbeid(BigDecimal verdi) {
        return new Stillingsprosent(normaliserData(verdi));
    }

    public BigDecimal skalertVerdi() {
        return verdi.setScale(2, AVRUNDINGSMODUS);
    }

    private static BigDecimal normaliserData(BigDecimal verdi) {
        if (verdi == null) {
            return null;
        }
        if (verdi.compareTo(BigDecimal.ZERO) < 0) {
            verdi = verdi.abs();
        }
        while (verdi.compareTo(Stillingsprosent.ARBEID_MAX_VERDI) > 0) {
            verdi = verdi.divide(BigDecimal.TEN, 2, AVRUNDINGSMODUS);
        }
        return verdi;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        return obj instanceof Stillingsprosent other && skalertVerdi().compareTo(other.skalertVerdi()) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(skalertVerdi());
    }

    @Override
    public int compareTo(Stillingsprosent o) {
        return this.verdi.compareTo(o.verdi);
    }

}
