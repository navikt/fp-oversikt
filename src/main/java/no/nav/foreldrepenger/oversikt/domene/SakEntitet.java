package no.nav.foreldrepenger.oversikt.domene;

import static no.nav.foreldrepenger.oversikt.server.JsonUserType.MAPPER;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import no.nav.foreldrepenger.oversikt.server.JsonUserType;

@Entity(name = "sak")
@Table(name = "sak")
@TypeDef(name = "jsonb", typeClass = JsonUserType.class)
public class SakEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SAK")
    private Long id;

    @Column(name = "saksnummer")
    private String saksnummer;

    @Column
    @Type(type = "jsonb")
    private ObjectNode json;

    public SakEntitet(Sak sak) {
        this.json = toObjectNode(sak);
        this.saksnummer = sak.saksnummer().value();
    }

    private static ObjectNode toObjectNode(Sak sak) {
        return MAPPER.valueToTree(sak);
    }

    protected SakEntitet() {
    }

    void setJson(Sak sak) {
        this.json = toObjectNode(sak);
    }

    Sak map() {
        try {
            return MAPPER.treeToValue(json, Sak.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Problemer med å deserialisere sak", e);
        }
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
