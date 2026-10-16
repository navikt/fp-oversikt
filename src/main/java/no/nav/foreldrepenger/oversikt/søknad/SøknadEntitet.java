package no.nav.foreldrepenger.oversikt.søknad;

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

@Entity(name = "soknad")
@Table(name = "soknad")
public class SøknadEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SOKNAD")
    @SequenceGenerator(name = "SEQ_SOKNAD", sequenceName = "SEQ_SOKNAD")
    @Column(name = "id")
    private Long id;

    @Column(name = "saksnummer", nullable = false)
    private String saksnummer;

    @JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
    @Column(name = "mottatt_tidspunkt", nullable = false)
    private Instant mottattTidspunkt;

    @Enumerated(EnumType.STRING)
    @Column(name = "rolle", nullable = false)
    private Søknad.Rolle rolle;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv;

    @OneToMany(mappedBy = "soknad", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private List<SøknadPeriodeEntitet> perioder = new ArrayList<>();

    SøknadEntitet(Saksnummer saksnummer, Søknad søknad) {
        this.saksnummer = saksnummer.value();
        this.mottattTidspunkt = søknad.mottattTidspunkt();
        this.rolle = søknad.rolle();
        this.aktiv = true;
        for (var periode : søknad.perioder()) {
            perioder.add(new SøknadPeriodeEntitet(this, periode));
        }
    }

    protected SøknadEntitet() {
    }

    Søknad map() {
        return new Søknad(mottattTidspunkt, rolle, perioder.stream().map(SøknadPeriodeEntitet::map).toList());
    }
}
