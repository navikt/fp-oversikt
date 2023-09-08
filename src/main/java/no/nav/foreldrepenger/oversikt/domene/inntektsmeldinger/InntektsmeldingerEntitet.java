package no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger;

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

@Entity(name = "inntektsmeldinger")
@Table(name = "inntektsmeldinger")
public class InntektsmeldingerEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_INNTEKTSMELDINGER")
    private Long id;

    @Column(name = "saksnummer")
    private String saksnummer;

    @Column(name = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    private Set<Inntektsmelding> json;

    public InntektsmeldingerEntitet(Saksnummer saksnummer, Set<Inntektsmelding> json) {
        this.json = json;
        this.saksnummer = saksnummer.value();
    }

    protected InntektsmeldingerEntitet() {
    }

    void setJson(Set<Inntektsmelding> inntektsmeldinger) {
        this.json = inntektsmeldinger;
    }

    public Set<Inntektsmelding> map() {
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        InntektsmeldingerEntitet that = (InntektsmeldingerEntitet) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
