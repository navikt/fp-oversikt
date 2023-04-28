package no.nav.foreldrepenger.oversikt;

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

@Entity
@Table(name = "vedtak")
@TypeDef(name = "jsonb", typeClass = JsonUserType.class)
public class VedtakEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_VEDTAK")
    private Long id;

    @Column
    @Type(type = "jsonb")
    private ObjectNode json;

    public VedtakEntitet(Vedtak vedtak) {
        this.json = MAPPER.valueToTree(vedtak);
    }

    protected VedtakEntitet() {
    }

    Vedtak map() {
        try {
            return MAPPER.treeToValue(json, Vedtak.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        VedtakEntitet that = (VedtakEntitet) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
