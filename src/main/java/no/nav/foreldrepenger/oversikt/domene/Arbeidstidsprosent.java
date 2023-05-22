package no.nav.foreldrepenger.oversikt.domene;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class Arbeidstidsprosent implements Comparable<Arbeidstidsprosent> {

    public static final Arbeidstidsprosent ZERO = new Arbeidstidsprosent(BigDecimal.ZERO);

    @JsonValue
    private final BigDecimal verdi;

    @JsonCreator
    public Arbeidstidsprosent(BigDecimal verdi) {
        Objects.requireNonNull(verdi);
        this.verdi = verdi.setScale(2, RoundingMode.DOWN);
    }

    public Arbeidstidsprosent(int verdi) {
        this(BigDecimal.valueOf(verdi));
    }

    public BigDecimal decimalValue() {
        return verdi.setScale(2, RoundingMode.DOWN);
    }

    @Override
    public String toString() {
        return verdi.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (Arbeidstidsprosent) o;
        return Objects.equals(decimalValue(), that.decimalValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(decimalValue());
    }

    @Override
    public int compareTo(Arbeidstidsprosent trekkdager) {
        return decimalValue().compareTo(trekkdager.decimalValue());
    }

    public boolean merEnn0() {
        return decimalValue().compareTo(BigDecimal.ZERO) > 0;
    }
}
