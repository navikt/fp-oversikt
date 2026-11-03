package no.nav.foreldrepenger.oversikt.soknadsplan;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

@Entity(name = "soknadsplan")
@Table(name = "soknadsplan")
public class SøknadsplanEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SOKNADSPLAN")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "saksnummer", nullable = false)
    private String saksnummer;

    @NotNull
    @Column(name = "mottatt_tidspunkt", nullable = false)
    private LocalDateTime mottattTidspunkt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "rolle", nullable = false)
    private Søknadsplan.Rolle rolle;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv;

    @OneToMany(mappedBy = "soknad", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @OrderBy("fom, tom")
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

    void deaktiver() {
        this.aktiv = false;
    }

    Søknadsplan map() {
        return new Søknadsplan(mottattTidspunkt, rolle, perioder.stream().map(SøknadsplanPeriodeEntitet::map).toList());
    }
}
