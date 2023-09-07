package no.nav.foreldrepenger.oversikt.domene;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class Beløp {

    public static final Beløp ZERO = new Beløp(BigDecimal.ZERO);
    @JsonValue
    private final BigDecimal verdi;

    @JsonCreator
    public Beløp(BigDecimal verdi) {
        Objects.requireNonNull(verdi);
        this.verdi = verdi.setScale(2, RoundingMode.HALF_EVEN);
    }

    public Beløp(int verdi) {
        this(BigDecimal.valueOf(verdi));
    }

    public BigDecimal decimalValue() {
        return verdi.setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public String toString() {
        return verdi.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (Beløp) o;
        return Objects.equals(decimalValue(), that.decimalValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(decimalValue());
    }
}
