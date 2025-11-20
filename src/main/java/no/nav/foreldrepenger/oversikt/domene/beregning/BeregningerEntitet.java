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

@Entity(name = "beregninger")
@Table(name = "beregninger")
public class BeregningerEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BEREGNING")
    private Long id;

    @Column(name = "saksnummer")
    private String saksnummer;

    @Column(name = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    private Set<Beregning> json;

    public BeregningerEntitet(Saksnummer saksnummer, Set<Beregning> json) {
        this.json = json;
        this.saksnummer = saksnummer.value();
    }

    protected BeregningerEntitet() {
    }

    void setJson(Set<Beregning> beregninger) {
        this.json = beregninger;
    }

    public Set<Beregning> map() {
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BeregningerEntitet that = (BeregningerEntitet) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
