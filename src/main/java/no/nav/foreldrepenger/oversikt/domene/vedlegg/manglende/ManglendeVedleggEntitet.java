package no.nav.foreldrepenger.oversikt.domene.vedlegg.manglende;

import java.util.List;
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
import no.nav.foreldrepenger.oversikt.arkiv.DokumentTypeId;

@Entity(name = "manglendeVedlegg")
@Table(name = "manglende_vedlegg")
public class ManglendeVedleggEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MANGLENDE_VEDLEGG")
    private Long id;

    @Column(name = "saksnummer")
    private String saksnummer;


    @Column(name = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<DokumentTypeId> json;

    public ManglendeVedleggEntitet(Saksnummer saksnummer, List<DokumentTypeId> json) {
        this.saksnummer = saksnummer.value();
        this.json = json;
    }

    protected ManglendeVedleggEntitet() {
    }

    public List<DokumentTypeId> manglendeVedlegg() {
        return json;
    }

    public ManglendeVedleggEntitet setJson(List<DokumentTypeId> manglendeVedlegg) {
        this.json = manglendeVedlegg;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ManglendeVedleggEntitet that = (ManglendeVedleggEntitet) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
