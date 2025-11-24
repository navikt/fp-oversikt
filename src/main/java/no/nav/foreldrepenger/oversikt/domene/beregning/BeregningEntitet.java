package no.nav.foreldrepenger.oversikt.domene.beregning;

import java.util.Objects;
import java.util.Set;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

@Entity(name = "beregning")
@Table(name = "beregning")
public class BeregningEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BEREGNING")
    private Long id;

    @Column(name = "saksnummer")
    private String saksnummer;

    @Column(name = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    private Beregning json;

    public BeregningEntitet(Saksnummer saksnummer, Beregning json) {
        this.json = json;
        this.saksnummer = saksnummer.value();
    }

    protected BeregningEntitet() {
    }

    void setJson(Beregning beregning) {
        this.json = beregning;
    }

    public Beregning map() {
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BeregningEntitet that = (BeregningEntitet) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
