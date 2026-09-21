package no.nav.foreldrepenger.oversikt.uttaksplan;

import java.util.Objects;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

@Entity(name = "annen_part_uttaksplan")
@Table(name = "annen_part_uttaksplan")
public class AnnenPartUttaksplanEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ANNEN_PART_UTTAKSPLAN")
    private Long id;

    @Column(name = "saksnummer")
    private String saksnummer;

    @Column(name = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    private AnnenPartUttaksplan json;

    public AnnenPartUttaksplanEntitet(Saksnummer saksnummer, AnnenPartUttaksplan json) {
        this.saksnummer = saksnummer.value();
        this.json = json;
    }

    protected AnnenPartUttaksplanEntitet() {
    }

    void setJson(AnnenPartUttaksplan uttaksplan) {
        this.json = uttaksplan;
    }

    AnnenPartUttaksplan map() {
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AnnenPartUttaksplanEntitet that = (AnnenPartUttaksplanEntitet) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
