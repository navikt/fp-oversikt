package no.nav.foreldrepenger.oversikt.domene.tilbakekreving;

import java.util.Objects;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "tilbakekreving")
@Table(name = "tilbakekreving")
public class TilbakekrevingEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TILBAKEKREVING")
    private Long id;

    @Column(name = "saksnummer")
    private String saksnummer;

    @Column(name = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    private Tilbakekreving json;

    public TilbakekrevingEntitet(Tilbakekreving json) {
        this.json = json;
        this.saksnummer = json.saksnummer().value();
    }

    protected TilbakekrevingEntitet() {
    }

    void setJson(Tilbakekreving tilbakekreving) {
        this.json = tilbakekreving;
    }

    public Tilbakekreving map() {
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TilbakekrevingEntitet that = (TilbakekrevingEntitet) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
