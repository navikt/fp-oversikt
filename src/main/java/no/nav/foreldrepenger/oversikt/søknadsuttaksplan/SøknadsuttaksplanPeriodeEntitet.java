package no.nav.foreldrepenger.oversikt.søknadsuttaksplan;

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

@Entity(name = "soknadsuttaksplan_periode")
@Table(name = "soknadsuttaksplan_periode")
public class SøknadsuttaksplanPeriodeEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SOKNADSUTTAKSPLAN_PERIODE")
    @SequenceGenerator(name = "SEQ_SOKNADSUTTAKSPLAN_PERIODE", sequenceName = "SEQ_SOKNADSUTTAKSPLAN_PERIODE")
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "soknadsuttaksplan_id", nullable = false)
    private SøknadsuttaksplanEntitet soknad;

    @Column(name = "fom", nullable = false)
    private LocalDate fom;

    @Column(name = "tom", nullable = false)
    private LocalDate tom;

    @Enumerated(EnumType.STRING)
    @Column(name = "rolle", nullable = false)
    private Søknadsuttaksplan.Rolle rolle;

    @Enumerated(EnumType.STRING)
    @Column(name = "konto_type")
    private SøknadsuttaksplanPeriode.KontoType kontoType;

    @Enumerated(EnumType.STRING)
    @Column(name = "utsettelse_arsak")
    private SøknadsuttaksplanPeriode.UtsettelseÅrsak utsettelseÅrsak;

    @Enumerated(EnumType.STRING)
    @Column(name = "overforing_arsak")
    private SøknadsuttaksplanPeriode.OverføringÅrsak overføringÅrsak;

    @Enumerated(EnumType.STRING)
    @Column(name = "mors_aktivitet")
    private SøknadsuttaksplanPeriode.MorsAktivitet morsAktivitet;

    @Column(name = "gradering_arbeidstidprosent")
    private BigDecimal graderingArbeidstidprosent;

    @Column(name = "samtidig_uttaksprosent")
    private BigDecimal samtidigUttaksprosent;

    @Column(name = "flerbarnsdager")
    private Boolean flerbarnsdager;

    @Enumerated(EnumType.STRING)
    @Column(name = "aktivitet_type", length = 50)
    private SøknadsuttaksplanPeriode.AktivitetType aktivitetType;

    @Column(name = "arbeidsgiver_id", length = 50)
    private String arbeidsgiverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "arbeidsgiver_type", length = 50)
    private SøknadsuttaksplanPeriode.ArbeidsgiverType arbeidsgiverType;

    SøknadsuttaksplanPeriodeEntitet(SøknadsuttaksplanEntitet soknad, SøknadsuttaksplanPeriode periode) {
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
        this.arbeidsgiverType = periode.arbeidsgiverType();
    }

    protected SøknadsuttaksplanPeriodeEntitet() {
    }

    SøknadsuttaksplanPeriode map() {
        return new SøknadsuttaksplanPeriode(fom, tom, rolle, kontoType, utsettelseÅrsak, overføringÅrsak, morsAktivitet,
            graderingArbeidstidprosent, samtidigUttaksprosent, flerbarnsdager, aktivitetType, arbeidsgiverId, arbeidsgiverType);
    }
}
