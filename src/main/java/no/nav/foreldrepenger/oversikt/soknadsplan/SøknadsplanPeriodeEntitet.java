package no.nav.foreldrepenger.oversikt.soknadsplan;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity(name = "soknadsplan_periode")
@Table(name = "soknadsplan_periode")
public class SøknadsplanPeriodeEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SOKNADSPLAN_PERIODE")
    @SequenceGenerator(name = "SEQ_SOKNADSPLAN_PERIODE", sequenceName = "SEQ_SOKNADSPLAN_PERIODE")
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "soknadsplan_id", nullable = false)
    private SøknadsplanEntitet soknad;

    @Column(name = "fom", nullable = false)
    private LocalDate fom;

    @Column(name = "tom", nullable = false)
    private LocalDate tom;

    @Enumerated(EnumType.STRING)
    @Column(name = "rolle", nullable = false)
    private Søknadsplan.Rolle rolle;

    @Enumerated(EnumType.STRING)
    @Column(name = "konto_type")
    private SøknadsplanPeriode.KontoType kontoType;

    @Enumerated(EnumType.STRING)
    @Column(name = "utsettelse_arsak")
    private SøknadsplanPeriode.UtsettelseÅrsak utsettelseÅrsak;

    @Enumerated(EnumType.STRING)
    @Column(name = "overforing_arsak")
    private SøknadsplanPeriode.OverføringÅrsak overføringÅrsak;

    @Enumerated(EnumType.STRING)
    @Column(name = "mors_aktivitet")
    private SøknadsplanPeriode.MorsAktivitet morsAktivitet;

    @Column(name = "gradering_arbeidstidprosent")
    private BigDecimal graderingArbeidstidprosent;

    @Column(name = "samtidig_uttaksprosent")
    private BigDecimal samtidigUttaksprosent;

    @Column(name = "flerbarnsdager")
    private Boolean flerbarnsdager;

    @Enumerated(EnumType.STRING)
    @Column(name = "aktivitet_type", length = 50)
    private SøknadsplanPeriode.AktivitetType aktivitetType;

    @Column(name = "arbeidsgiver_id", length = 50)
    private String arbeidsgiverId;

    SøknadsplanPeriodeEntitet(SøknadsplanEntitet soknad, SøknadsplanPeriode periode) {
        this.soknad = soknad;
        this.fom = periode.fom();
        this.tom = periode.tom();
        this.rolle = periode.rolle();
        this.kontoType = periode.kontoType();
        this.utsettelseÅrsak = periode.utsettelseÅrsak();
        this.overføringÅrsak = periode.overføringÅrsak();
        this.morsAktivitet = periode.morsAktivitet();
        this.graderingArbeidstidprosent = periode.graderingArbeidstidprosent();
        this.samtidigUttaksprosent = periode.samtidigUttaksprosent();
        this.flerbarnsdager = periode.flerbarnsdager();
        this.aktivitetType = periode.aktivitetType();
        this.arbeidsgiverId = periode.arbeidsgiverId();
    }

    protected SøknadsplanPeriodeEntitet() {
    }

    SøknadsplanPeriode map() {
        return new SøknadsplanPeriode(fom, tom, rolle, kontoType, utsettelseÅrsak, overføringÅrsak, morsAktivitet,
            graderingArbeidstidprosent, samtidigUttaksprosent, flerbarnsdager, aktivitetType, arbeidsgiverId);
    }
}
