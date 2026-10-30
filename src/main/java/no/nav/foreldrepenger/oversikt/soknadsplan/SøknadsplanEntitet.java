package no.nav.foreldrepenger.oversikt.soknadsplan;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

@Entity(name = "soknadsplan")
@Table(name = "soknadsplan")
public class SøknadsplanEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SOKNADSPLAN")
    @SequenceGenerator(name = "SEQ_SOKNADSPLAN", sequenceName = "SEQ_SOKNADSPLAN")
    @Column(name = "id")
    private Long id;

    @Column(name = "saksnummer", nullable = false)
    private String saksnummer;

    @JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
    @Column(name = "mottatt_tidspunkt", nullable = false)
    private Instant mottattTidspunkt;

    @Enumerated(EnumType.STRING)
    @Column(name = "rolle", nullable = false)
    private Søknadsplan.Rolle rolle;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv;

    @OneToMany(mappedBy = "soknad", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private List<SøknadsplanPeriodeEntitet> perioder = new ArrayList<>();

    SøknadsplanEntitet(Saksnummer saksnummer, Søknadsplan søknad) {
        this.saksnummer = saksnummer.value();
        this.mottattTidspunkt = søknad.mottattTidspunkt();
        this.rolle = søknad.rolle();
        this.aktiv = true;
        for (var periode : søknad.perioder()) {
            perioder.add(new SøknadsplanPeriodeEntitet(this, periode));
        }
    }

    protected SøknadsplanEntitet() {
    }

    Søknadsplan map() {
        return new Søknadsplan(mottattTidspunkt, rolle, perioder.stream().map(SøknadsplanPeriodeEntitet::map).toList());
    }
}
