package no.nav.foreldrepenger.oversikt.domene;

import java.util.Objects;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "sak")
@Table(name = "sak")
public class SakEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SAK")
    private Long id;

    @Column(name = "saksnummer")
    private String saksnummer;

    @Column(name = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    private Sak json;

    public SakEntitet(Sak json) {
        this.json = json;
        this.saksnummer = json.saksnummer().value();
    }

    protected SakEntitet() {
    }

    void setJson(Sak sak) {
        this.json = sak;
    }

    public Sak map() {
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SakEntitet that = (SakEntitet) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
