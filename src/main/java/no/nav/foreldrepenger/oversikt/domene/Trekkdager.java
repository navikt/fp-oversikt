package no.nav.foreldrepenger.oversikt.domene;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class Trekkdager implements Comparable<Trekkdager> {

    public static final Trekkdager ZERO = new Trekkdager(BigDecimal.ZERO);

    @JsonValue
    private final BigDecimal verdi;

    @JsonCreator
    public Trekkdager(BigDecimal verdi) {
        Objects.requireNonNull(verdi);
        this.verdi = verdi.setScale(1, RoundingMode.DOWN);
    }

    public Trekkdager(int verdi) {
        this(BigDecimal.valueOf(verdi));
    }

    public BigDecimal decimalValue() {
        return verdi.setScale(1, RoundingMode.DOWN);
    }

    @Override
    public String toString() {
        return verdi.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (Trekkdager) o;
        return Objects.equals(decimalValue(), that.decimalValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(decimalValue());
    }

    public boolean merEnn0() {
        return decimalValue().compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public int compareTo(Trekkdager trekkdager) {
        return decimalValue().compareTo(trekkdager.decimalValue());
    }
}
