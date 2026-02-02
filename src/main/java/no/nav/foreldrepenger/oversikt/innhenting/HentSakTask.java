package no.nav.foreldrepenger.oversikt.innhenting;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.foreldrepenger.oversikt.domene.fp.AktivitetStatus;
import no.nav.foreldrepenger.oversikt.domene.fp.Beregningsgrunnlag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.domene.Aksjonspunkt;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.SøknadStatus;
import no.nav.foreldrepenger.oversikt.domene.es.EsSøknad;
import no.nav.foreldrepenger.oversikt.domene.es.EsVedtak;
import no.nav.foreldrepenger.oversikt.domene.es.SakES0;
import no.nav.foreldrepenger.oversikt.domene.fp.BrukerRolle;
import no.nav.foreldrepenger.oversikt.domene.fp.Dekningsgrad;
import no.nav.foreldrepenger.oversikt.domene.fp.FpSøknad;
import no.nav.foreldrepenger.oversikt.domene.fp.FpSøknadsperiode;
import no.nav.foreldrepenger.oversikt.domene.fp.FpVedtak;
import no.nav.foreldrepenger.oversikt.domene.fp.Gradering;
import no.nav.foreldrepenger.oversikt.domene.fp.Konto;
import no.nav.foreldrepenger.oversikt.domene.fp.MorsAktivitet;
import no.nav.foreldrepenger.oversikt.domene.fp.OppholdÅrsak;
import no.nav.foreldrepenger.oversikt.domene.fp.OverføringÅrsak;
import no.nav.foreldrepenger.oversikt.domene.fp.Rettigheter;
import no.nav.foreldrepenger.oversikt.domene.fp.SakFP0;
import no.nav.foreldrepenger.oversikt.domene.fp.TilkjentYtelse;
import no.nav.foreldrepenger.oversikt.domene.fp.UtsettelseÅrsak;
import no.nav.foreldrepenger.oversikt.domene.fp.UttakAktivitet;
import no.nav.foreldrepenger.oversikt.domene.fp.UttakPeriodeAnnenpartEøs;
import no.nav.foreldrepenger.oversikt.domene.fp.Uttaksperiode;
import no.nav.foreldrepenger.oversikt.domene.svp.Aktivitet;
import no.nav.foreldrepenger.oversikt.domene.svp.ArbeidsforholdUttak;
import no.nav.foreldrepenger.oversikt.domene.svp.OppholdPeriode;
import no.nav.foreldrepenger.oversikt.domene.svp.ResultatÅrsak;
import no.nav.foreldrepenger.oversikt.domene.svp.SakSVP0;
import no.nav.foreldrepenger.oversikt.domene.svp.SvpPeriode;
import no.nav.foreldrepenger.oversikt.domene.svp.SvpSøknad;
import no.nav.foreldrepenger.oversikt.domene.svp.SvpVedtak;
import no.nav.foreldrepenger.oversikt.domene.svp.Tilrettelegging;
import no.nav.foreldrepenger.oversikt.domene.svp.TilretteleggingPeriode;
import no.nav.foreldrepenger.oversikt.domene.svp.TilretteleggingType;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "hent.sak", prioritet = 2)
public class HentSakTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HentSakTask.class);

    private final FpsakTjeneste fpSakKlient;
    private final SakRepository sakRepository;

    @Inject
    public HentSakTask(FpsakTjeneste fpsakTjeneste, SakRepository sakRepository) {
        this.fpSakKlient = fpsakTjeneste;
        this.sakRepository = sakRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        hentOgLagreSak(fpSakKlient, sakRepository, new Saksnummer(prosessTaskData.getSaksnummer()));
    }

    public static void hentOgLagreSak(FpsakTjeneste fpsak, SakRepository repository, Saksnummer saksnummer) {
        var sakDto = fpsak.hentSak(saksnummer);
        LOG.info("Hentet sak på {} fra fpsak", saksnummer.value());
        repository.lagre(map(sakDto));
    }

    static no.nav.foreldrepenger.oversikt.domene.Sak map(Sak sakDto) {
        if (sakDto == null) {
            throw new IllegalStateException("Hentet sak er null og kan ikke bli mappet!");
        }

        var familieHendelse = tilFamilieHendelse(sakDto.familieHendelse());
        var saksnummer = new Saksnummer(sakDto.saksnummer());
        var aktørId = new AktørId(sakDto.aktørId());
        var aksjonspunkt = tilAksjonspunkt(sakDto.aksjonspunkt());
        var oppdatertTidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        if (sakDto instanceof FpSak fpsak) {
            var annenPart = fpsak.oppgittAnnenPart() == null ? null : new AktørId(fpsak.oppgittAnnenPart());
            var søknader = fpsak.søknader().stream().map(HentSakTask::tilFpSøknad).collect(Collectors.toSet());
            var brukerRolle = tilBrukerRolle(fpsak.brukerRolle());
            var fødteBarn = tilFødteBarn(fpsak.fødteBarn());
            return new SakFP0(saksnummer, aktørId, sakDto.avsluttet(), tilFpVedtak(fpsak.vedtak()), annenPart, familieHendelse, aksjonspunkt,
                søknader, brukerRolle, fødteBarn, tilRettigheter(fpsak.rettigheter()), fpsak.ønskerJustertUttakVedFødsel(), oppdatertTidspunkt);
        }
        if (sakDto instanceof SvpSak svpSak) {
            var søknader = svpSak.søknader().stream().map(HentSakTask::tilSvpSøknad).collect(Collectors.toSet());
            return new SakSVP0(saksnummer, aktørId, sakDto.avsluttet(), familieHendelse, aksjonspunkt, søknader, tilSvpVedtak(svpSak.vedtak()),
                oppdatertTidspunkt);
        }
        if (sakDto instanceof EsSak esSak) {
            var søknader = esSak.søknader().stream().map(HentSakTask::tilEsSøknad).collect(Collectors.toSet());
            return new SakES0(saksnummer, aktørId, sakDto.avsluttet(), familieHendelse, aksjonspunkt, søknader, tilEsVedtak(esSak.vedtak()),
                oppdatertTidspunkt);
        }

        throw new IllegalStateException("Hentet sak er null og kan ikke bli mappet!");
    }

    private static Set<SvpVedtak> tilSvpVedtak(Set<SvpSak.Vedtak> vedtak) {
        if (vedtak == null) {
            return Set.of();
        }
        return vedtak.stream().map(v -> {
            var arbeidsforhold = v.arbeidsforhold().stream().map(HentSakTask::tilArbeidsforhold).collect(Collectors.toSet());
            var avslagÅrsak = v.avslagÅrsak() == null ? null : switch (v.avslagÅrsak()) {
                case ARBEIDSGIVER_KAN_TILRETTELEGGE -> SvpVedtak.AvslagÅrsak.ARBEIDSGIVER_KAN_TILRETTELEGGE;
                case SØKER_ER_INNVILGET_SYKEPENGER -> SvpVedtak.AvslagÅrsak.SØKER_ER_INNVILGET_SYKEPENGER;
                case MANGLENDE_DOKUMENTASJON -> SvpVedtak.AvslagÅrsak.MANGLENDE_DOKUMENTASJON;
                case ANNET -> SvpVedtak.AvslagÅrsak.ANNET;
            };
            return new SvpVedtak(v.vedtakstidspunkt(), arbeidsforhold, avslagÅrsak);
        }).collect(Collectors.toSet());
    }

    private static ArbeidsforholdUttak tilArbeidsforhold(SvpSak.Vedtak.ArbeidsforholdUttak a) {
        var svpPerioder = a.svpPerioder().stream().map(HentSakTask::tilSvpPeriode).collect(Collectors.toSet());
        var oppholdsperioder = a.oppholdsperioder().stream().map(HentSakTask::tilOppholdPeriode).collect(Collectors.toSet());
        var ikkeOppfyltÅrsak = a.ikkeOppfyltÅrsak() == null ? null : switch (a.ikkeOppfyltÅrsak()) {
            case ARBEIDSGIVER_KAN_TILRETTELEGGE -> ArbeidsforholdUttak.ArbeidsforholdIkkeOppfyltÅrsak.ARBEIDSGIVER_KAN_TILRETTELEGGE;
            case ARBEIDSGIVER_KAN_TILRETTELEGGE_FREM_TIL_3_UKER_FØR_TERMIN ->
                ArbeidsforholdUttak.ArbeidsforholdIkkeOppfyltÅrsak.ARBEIDSGIVER_KAN_TILRETTELEGGE_FREM_TIL_3_UKER_FØR_TERMIN;
            case ANNET -> ArbeidsforholdUttak.ArbeidsforholdIkkeOppfyltÅrsak.ANNET;
        };
        return new ArbeidsforholdUttak(tilAktivitet(a.aktivitet()), a.behovFom(), a.risikoFaktorer(), a.tiltak(), svpPerioder, oppholdsperioder,
            ikkeOppfyltÅrsak);
    }

    private static SvpPeriode tilSvpPeriode(SvpSak.Vedtak.ArbeidsforholdUttak.SvpPeriode periode) {
        var resultatÅrsak = switch (periode.resultatÅrsak()) {
            case INNVILGET -> ResultatÅrsak.INNVILGET;
            case AVSLAG_SØKNADSFRIST -> ResultatÅrsak.AVSLAG_SØKNADSFRIST;
            case AVSLAG_ANNET -> ResultatÅrsak.AVSLAG_ANNET;
            case AVSLAG_INNGANGSVILKÅR -> ResultatÅrsak.AVSLAG_INNGANGSVILKÅR;
            case OPPHØR_OVERGANG_FORELDREPENGER -> ResultatÅrsak.OPPHØR_OVERGANG_FORELDREPENGER;
            case OPPHØR_FØDSEL -> ResultatÅrsak.OPPHØR_FØDSEL;
            case OPPHØR_TIDSPERIODE_FØR_TERMIN -> ResultatÅrsak.OPPHØR_TIDSPERIODE_FØR_TERMIN;
            case OPPHØR_OPPHOLD_I_YTELSEN -> ResultatÅrsak.OPPHØR_OPPHOLD_I_YTELSEN;
            case OPPHØR_ANNET -> ResultatÅrsak.OPPHØR_ANNET;
        };
        return new SvpPeriode(periode.fom(), periode.tom(), tilTilretteleggingType(periode.tilretteleggingType()), periode.arbeidstidprosent(),
            periode.utbetalingsgrad(), resultatÅrsak);
    }

    private static Set<EsVedtak> tilEsVedtak(Set<EsSak.Vedtak> vedtak) {
        return Stream.ofNullable(vedtak).flatMap(Collection::stream).map(v -> new EsVedtak(v.vedtakstidspunkt())).collect(Collectors.toSet());
    }

    private static Rettigheter tilRettigheter(FpSak.Rettigheter rettigheter) {
        if (rettigheter == null) {
            return null;
        }
        return new Rettigheter(rettigheter.aleneomsorg(), rettigheter.morUføretrygd(), rettigheter.annenForelderTilsvarendeRettEØS());
    }

    private static Set<AktørId> tilFødteBarn(Set<String> barn) {
        return barn.stream().map(AktørId::new).collect(Collectors.toSet());
    }

    private static BrukerRolle tilBrukerRolle(FpSak.BrukerRolle brukerRolle) {
        return switch (brukerRolle) {
            case MOR -> BrukerRolle.MOR;
            case FAR -> BrukerRolle.FAR;
            case MEDMOR -> BrukerRolle.MEDMOR;
            case UKJENT -> BrukerRolle.UKJENT;
        };
    }

    private static EsSøknad tilEsSøknad(EsSak.Søknad søknad) {
        return new EsSøknad(map(søknad.status()), søknad.mottattTidspunkt());
    }

    private static SvpSøknad tilSvpSøknad(SvpSak.Søknad søknad) {
        return new SvpSøknad(map(søknad.status()), søknad.mottattTidspunkt(), tilTilrettelegginger(søknad.tilrettelegginger()));
    }

    private static Set<Tilrettelegging> tilTilrettelegginger(Set<SvpSak.Søknad.Tilrettelegging> tilrettelegginger) {
        return tilrettelegginger.stream()
            .map(t -> new Tilrettelegging(tilAktivitet(t.aktivitet()), t.behovFom(), t.risikoFaktorer(), t.tiltak(),
                t.perioder().stream().map(HentSakTask::tilTilrettleggingPeriode).collect(Collectors.toSet()),
                t.oppholdsperioder().stream().map(HentSakTask::tilOppholdPeriode).collect(Collectors.toSet())))
            .collect(Collectors.toSet());
    }

    private static OppholdPeriode tilOppholdPeriode(SvpSak.OppholdPeriode oppholdPeriode) {
        return new OppholdPeriode(oppholdPeriode.fom(), oppholdPeriode.tom(), switch (oppholdPeriode.årsak()) {
            case FERIE -> OppholdPeriode.Årsak.FERIE;
            case SYKEPENGER -> OppholdPeriode.Årsak.SYKEPENGER;
        }, switch (oppholdPeriode.kilde()) {
            case SAKSBEHANDLER -> OppholdPeriode.OppholdKilde.SAKSBEHANDLER;
            case INNTEKTSMELDING -> OppholdPeriode.OppholdKilde.INNTEKTSMELDING;
            case SØKNAD -> OppholdPeriode.OppholdKilde.SØKNAD;
        });
    }

    private static TilretteleggingPeriode tilTilrettleggingPeriode(SvpSak.Søknad.Tilrettelegging.Periode periode) {
        return new TilretteleggingPeriode(periode.fom(), tilTilretteleggingType(periode.type()), periode.arbeidstidprosent());
    }

    private static TilretteleggingType tilTilretteleggingType(SvpSak.TilretteleggingType type) {
        return switch (type) {
            case HEL -> TilretteleggingType.HEL;
            case DELVIS -> TilretteleggingType.DELVIS;
            case INGEN -> TilretteleggingType.INGEN;
        };
    }

    private static Aktivitet tilAktivitet(SvpSak.Aktivitet aktivitet) {
        var type = switch (aktivitet.type()) {
            case ORDINÆRT_ARBEID -> Aktivitet.Type.ORDINÆRT_ARBEID;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> Aktivitet.Type.SELVSTENDIG_NÆRINGSDRIVENDE;
            case FRILANS -> Aktivitet.Type.FRILANS;
        };
        var arbeidsgiver = tilArbeidsgiver(aktivitet.arbeidsgiver());
        return new Aktivitet(type, arbeidsgiver, aktivitet.arbeidsforholdId(), aktivitet.arbeidsgiverNavn());
    }

    private static FpSøknad tilFpSøknad(FpSak.Søknad søknad) {
        var perioder = søknad.perioder().stream().map(HentSakTask::tilSøknadsperiode).collect(Collectors.toSet());
        return new FpSøknad(map(søknad.status()), søknad.mottattTidspunkt(), perioder, tilDekningsgrad(søknad.dekningsgrad()),
            søknad.morArbeidUtenDok());
    }

    private static SøknadStatus map(no.nav.foreldrepenger.oversikt.innhenting.SøknadStatus status) {
        return switch (status) {
            case MOTTATT -> SøknadStatus.MOTTATT;
            case BEHANDLET -> SøknadStatus.BEHANDLET;
        };
    }

    private static FpSøknadsperiode tilSøknadsperiode(FpSak.Søknad.Periode periode) {
        var utsettelseÅrsak = periode.utsettelseÅrsak() == null ? null : map(periode.utsettelseÅrsak());
        var oppholdÅrsak = periode.oppholdÅrsak() == null ? null : map(periode.oppholdÅrsak());
        var overføringÅrsak = periode.overføringÅrsak() == null ? null : map(periode.overføringÅrsak());
        var gradering = mapGradering(periode.gradering());
        var morsAktivitet = periode.morsAktivitet() == null ? null : map(periode.morsAktivitet());
        return new FpSøknadsperiode(periode.fom(), periode.tom(), tilKonto(periode.konto()), utsettelseÅrsak, oppholdÅrsak, overføringÅrsak,
            gradering, periode.samtidigUttak(), periode.flerbarnsdager(), morsAktivitet);
    }

    private static MorsAktivitet map(no.nav.foreldrepenger.oversikt.innhenting.MorsAktivitet morsAktivitet) {
        return morsAktivitet == null ? null : switch (morsAktivitet) {
            case ARBEID -> MorsAktivitet.ARBEID;
            case UTDANNING -> MorsAktivitet.UTDANNING;
            case KVALPROG -> MorsAktivitet.KVALPROG;
            case INTROPROG -> MorsAktivitet.INTROPROG;
            case TRENGER_HJELP -> MorsAktivitet.TRENGER_HJELP;
            case INNLAGT -> MorsAktivitet.INNLAGT;
            case ARBEID_OG_UTDANNING -> MorsAktivitet.ARBEID_OG_UTDANNING;
            case UFØRE -> MorsAktivitet.UFØRE;
            case IKKE_OPPGITT -> MorsAktivitet.IKKE_OPPGITT;
        };
    }

    private static OverføringÅrsak map(no.nav.foreldrepenger.oversikt.innhenting.OverføringÅrsak overføringÅrsak) {
        return overføringÅrsak == null ? null : switch (overføringÅrsak) {
            case INSTITUSJONSOPPHOLD_ANNEN_FORELDER -> OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER;
            case SYKDOM_ANNEN_FORELDER -> OverføringÅrsak.SYKDOM_ANNEN_FORELDER;
            case IKKE_RETT_ANNEN_FORELDER -> OverføringÅrsak.IKKE_RETT_ANNEN_FORELDER;
            case ALENEOMSORG -> OverføringÅrsak.ALENEOMSORG;
        };
    }

    private static OppholdÅrsak map(no.nav.foreldrepenger.oversikt.innhenting.OppholdÅrsak oppholdÅrsak) {
        return oppholdÅrsak == null ? null : switch (oppholdÅrsak) {
            case MØDREKVOTE_ANNEN_FORELDER -> OppholdÅrsak.MØDREKVOTE_ANNEN_FORELDER;
            case FEDREKVOTE_ANNEN_FORELDER -> OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER;
            case FELLESPERIODE_ANNEN_FORELDER -> OppholdÅrsak.FELLESPERIODE_ANNEN_FORELDER;
            case FORELDREPENGER_ANNEN_FORELDER -> OppholdÅrsak.FORELDREPENGER_ANNEN_FORELDER;
        };
    }

    private static UtsettelseÅrsak map(no.nav.foreldrepenger.oversikt.innhenting.UtsettelseÅrsak utsettelseÅrsak) {
        return utsettelseÅrsak == null ? null : switch (utsettelseÅrsak) {
            case HV_ØVELSE -> UtsettelseÅrsak.HV_ØVELSE;
            case ARBEID -> UtsettelseÅrsak.ARBEID;
            case LOVBESTEMT_FERIE -> UtsettelseÅrsak.LOVBESTEMT_FERIE;
            case SØKER_SYKDOM -> UtsettelseÅrsak.SØKER_SYKDOM;
            case SØKER_INNLAGT -> UtsettelseÅrsak.SØKER_INNLAGT;
            case BARN_INNLAGT -> UtsettelseÅrsak.BARN_INNLAGT;
            case NAV_TILTAK -> UtsettelseÅrsak.NAV_TILTAK;
            case FRI -> UtsettelseÅrsak.FRI;
        };
    }

    private static Gradering mapGradering(FpSak.Gradering gradering) {
        return gradering == null ? null : new Gradering(gradering.prosent(), mapAktivitet(gradering.uttakAktivitet()));
    }

    private static UttakAktivitet mapAktivitet(FpSak.UttakAktivitet uttakAktivitet) {
        return new UttakAktivitet(switch (uttakAktivitet.type()) {
            case ORDINÆRT_ARBEID -> UttakAktivitet.Type.ORDINÆRT_ARBEID;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> UttakAktivitet.Type.SELVSTENDIG_NÆRINGSDRIVENDE;
            case FRILANS -> UttakAktivitet.Type.FRILANS;
            case ANNET -> UttakAktivitet.Type.ANNET;
        }, uttakAktivitet.arbeidsgiver(), uttakAktivitet.arbeidsforholdId());
    }

    private static Konto tilKonto(no.nav.foreldrepenger.oversikt.innhenting.Konto konto) {
        if (konto == null) {
            return null;
        }
        return switch (konto) {
            case FORELDREPENGER -> Konto.FORELDREPENGER;
            case MØDREKVOTE -> Konto.MØDREKVOTE;
            case FEDREKVOTE -> Konto.FEDREKVOTE;
            case FELLESPERIODE -> Konto.FELLESPERIODE;
            case FORELDREPENGER_FØR_FØDSEL -> Konto.FORELDREPENGER_FØR_FØDSEL;
        };
    }

    private static Set<Aksjonspunkt> tilAksjonspunkt(Set<Sak.Aksjonspunkt> aksjonspunkt) {
        return Stream.ofNullable(aksjonspunkt).flatMap(Collection::stream).map(a -> {
            var venteårsak = a.venteårsak() == null ? null : switch (a.venteårsak()) {
                case ANKE_VENTER_PÅ_MERKNADER_FRA_BRUKER -> Aksjonspunkt.Venteårsak.ANKE_VENTER_PÅ_MERKNADER_FRA_BRUKER;
                case AVVENT_DOKUMTANSJON -> Aksjonspunkt.Venteårsak.AVVENT_DOKUMTANSJON;
                case AVVENT_FØDSEL -> Aksjonspunkt.Venteårsak.AVVENT_FØDSEL;
                case AVVENT_RESPONS_REVURDERING -> Aksjonspunkt.Venteårsak.AVVENT_RESPONS_REVURDERING;
                case BRUKERTILBAKEMELDING -> Aksjonspunkt.Venteårsak.BRUKERTILBAKEMELDING;
                case UTLAND_TRYGD -> Aksjonspunkt.Venteårsak.UTLAND_TRYGD;
                case FOR_TIDLIG_SOKNAD -> Aksjonspunkt.Venteårsak.FOR_TIDLIG_SOKNAD;
                case UTVIDET_FRIST -> Aksjonspunkt.Venteårsak.UTVIDET_FRIST;
                case INNTEKT_RAPPORTERINGSFRIST -> Aksjonspunkt.Venteårsak.INNTEKT_RAPPORTERINGSFRIST;
                case MANGLENDE_SYKEMELDING -> Aksjonspunkt.Venteårsak.MANGLENDE_SYKEMELDING;
                case MANGLENDE_INNTEKTSMELDING -> Aksjonspunkt.Venteårsak.MANGLENDE_INNTEKTSMELDING;
                case OPPTJENING_OPPLYSNINGER -> Aksjonspunkt.Venteårsak.OPPTJENING_OPPLYSNINGER;
                case SISTE_AAP_ELLER_DP_MELDEKORT -> Aksjonspunkt.Venteårsak.SISTE_AAP_ELLER_DP_MELDEKORT;
                case SENDT_INFORMASJONSBREV -> Aksjonspunkt.Venteårsak.SENDT_INFORMASJONSBREV;
                case ÅPEN_BEHANDLING -> Aksjonspunkt.Venteårsak.ÅPEN_BEHANDLING;
            };
            var type = switch (a.type()) {
                case VENT_MANUELT_SATT -> Aksjonspunkt.Type.VENT_MANUELT_SATT;
                case VENT_FØDSEL -> Aksjonspunkt.Type.VENT_FØDSEL;
                case VENT_KOMPLETT_SØKNAD -> Aksjonspunkt.Type.VENT_KOMPLETT_SØKNAD;
                case VENT_REVURDERING -> Aksjonspunkt.Type.VENT_REVURDERING;
                case VENT_TIDLIG_SØKNAD -> Aksjonspunkt.Type.VENT_TIDLIG_SØKNAD;
                case VENT_KØET_BEHANDLING -> Aksjonspunkt.Type.VENT_KØET_BEHANDLING;
                case VENT_SØKNAD -> Aksjonspunkt.Type.VENT_SØKNAD;
                case VENT_INNTEKT_RAPPORTERINGSFRIST -> Aksjonspunkt.Type.VENT_INNTEKT_RAPPORTERINGSFRIST;
                case VENT_SISTE_AAP_ELLER_DP_MELDEKORT -> Aksjonspunkt.Type.VENT_SISTE_AAP_ELLER_DP_MELDEKORT;
                case VENT_ETTERLYST_INNTEKTSMELDING -> Aksjonspunkt.Type.VENT_ETTERLYST_INNTEKTSMELDING;
                case VENT_ANKE_OVERSENDT_TIL_TRYGDERETTEN -> Aksjonspunkt.Type.VENT_ANKE_OVERSENDT_TIL_TRYGDERETTEN;
                case VENT_SYKEMELDING -> Aksjonspunkt.Type.VENT_SYKEMELDING;
                case VENT_KABAL_KLAGE -> Aksjonspunkt.Type.VENT_KABAL_KLAGE;
                case VENT_PÅ_KABAL_ANKE -> Aksjonspunkt.Type.VENT_PÅ_KABAL_ANKE;
                case ANNET -> Aksjonspunkt.Type.ANNET;
            };
            return new Aksjonspunkt(type, venteårsak, a.tidsfrist());
        }).collect(Collectors.toSet());
    }

    private static FamilieHendelse tilFamilieHendelse(Sak.FamilieHendelse familiehendelse) {
        return familiehendelse == null ? null : new FamilieHendelse(familiehendelse.fødselsdato(), familiehendelse.termindato(),
            familiehendelse.antallBarn(), familiehendelse.omsorgsovertakelse());
    }

    private static Set<FpVedtak> tilFpVedtak(Set<FpSak.Vedtak> vedtakene) {
        return Stream.ofNullable(vedtakene).flatMap(Collection::stream).map(HentSakTask::tilFpVedtak).collect(Collectors.toSet());
    }

    private static FpVedtak tilFpVedtak(FpSak.Vedtak vedtakDto) {
        if (vedtakDto == null) {
            return null;
        }
        return new FpVedtak(vedtakDto.vedtakstidspunkt(), tilUttaksperiode(vedtakDto.uttaksperioder()), tilDekningsgrad(vedtakDto.dekningsgrad()),
            tilUttaksperiodeAnnenpartEøs(vedtakDto.annenpartEøsUttaksperioder()), tilBeregningsgrunnlag(vedtakDto.beregningsgrunnlag()),
            tilTilkjentYtelse(vedtakDto.tilkjentYtelse()));
    }

    private static Beregningsgrunnlag tilBeregningsgrunnlag(FpSak.Beregningsgrunnlag beregningsgrunnlag) {
        if (beregningsgrunnlag == null) {
            return null;
        }
        List<Beregningsgrunnlag.BeregningsAndel> andeler =
            beregningsgrunnlag.beregningsAndeler() == null ? List.of() : beregningsgrunnlag.beregningsAndeler()
                .stream()
                .map(HentSakTask::mapAndel)
                .toList();
        List<Beregningsgrunnlag.BeregningAktivitetStatus> statuser =
            beregningsgrunnlag.beregningAktivitetStatuser() == null ? List.of() : beregningsgrunnlag.beregningAktivitetStatuser()
                .stream()
                .map(HentSakTask::mapStatusMedHjemmel)
                .toList();
        return new Beregningsgrunnlag(beregningsgrunnlag.skjæringstidspunkt(), andeler, statuser, beregningsgrunnlag.grunnbeløp());
    }

    private static TilkjentYtelse tilTilkjentYtelse(FpSak.TilkjentYtelse tilkjentYtelse) {
        if (tilkjentYtelse == null) {
            return null;
        }
        var utbetalingsPerioder =
            tilkjentYtelse.utbetalingsPerioder() == null ? List.<TilkjentYtelse.TilkjentYtelsePeriode>of() : tilkjentYtelse.utbetalingsPerioder()
                .stream()
                .map(HentSakTask::tilTilkjentYtelsePeriode)
                .toList();
        var feriepenger = tilkjentYtelse.feriepenger() == null ? List.<TilkjentYtelse.FeriepengeAndel>of() : tilkjentYtelse.feriepenger()
            .stream()
            .map(HentSakTask::tilFeriepengeAndel)
            .toList();
        return new TilkjentYtelse(utbetalingsPerioder, feriepenger);
    }

    private static TilkjentYtelse.TilkjentYtelsePeriode tilTilkjentYtelsePeriode(FpSak.TilkjentYtelse.TilkjentYtelsePeriode periode) {
        var andeler = periode.andeler() == null ? List.<TilkjentYtelse.TilkjentYtelsePeriode.Andel>of() : periode.andeler()
            .stream()
            .map(HentSakTask::tilTilkjentYtelseAndel)
            .toList();
        return new TilkjentYtelse.TilkjentYtelsePeriode(periode.fom(), periode.tom(), andeler);
    }

    private static TilkjentYtelse.TilkjentYtelsePeriode.Andel tilTilkjentYtelseAndel(FpSak.TilkjentYtelse.TilkjentYtelsePeriode.Andel andel) {
        return new TilkjentYtelse.TilkjentYtelsePeriode.Andel(andel.aktivitetStatus(), andel.arbeidsgiverIdent(), andel.arbeidsgivernavn(),
            andel.dagsats(), andel.tilBruker(), andel.utbetalingsgrad());
    }

    private static TilkjentYtelse.FeriepengeAndel tilFeriepengeAndel(FpSak.TilkjentYtelse.FeriepengeAndel andel) {
        return new TilkjentYtelse.FeriepengeAndel(andel.opptjeningsår(), andel.årsbeløp(), andel.arbeidsgiverIdent(), andel.tilBruker());
    }

    private static Beregningsgrunnlag.BeregningAktivitetStatus mapStatusMedHjemmel(FpSak.Beregningsgrunnlag.BeregningAktivitetStatus statusMedHjemmelDto) {
        return new Beregningsgrunnlag.BeregningAktivitetStatus(mapAktivitetstatus(statusMedHjemmelDto.aktivitetStatus()),
            statusMedHjemmelDto.hjemmel());
    }

    private static Beregningsgrunnlag.BeregningsAndel mapAndel(FpSak.Beregningsgrunnlag.BeregningsAndel andelDto) {
        return new Beregningsgrunnlag.BeregningsAndel(mapAktivitetstatus(andelDto.aktivitetStatus()), andelDto.fastsattPrÅr(),
            mapInntektkilde(andelDto.inntektsKilde()), mapArbeidsforhold(andelDto.arbeidsforhold()), andelDto.dagsatsArbeidsgiver(),
            andelDto.dagsatsSøker());
    }

    private static Beregningsgrunnlag.Arbeidsforhold mapArbeidsforhold(FpSak.Beregningsgrunnlag.Arbeidsforhold arbeidsforholdDto) {
        if (arbeidsforholdDto == null) {
            return null;
        }
        return new Beregningsgrunnlag.Arbeidsforhold(arbeidsforholdDto.arbeidsgiverIdent(), arbeidsforholdDto.arbeidsgivernavn(),
            arbeidsforholdDto.refusjonPrMnd());
    }

    private static Beregningsgrunnlag.InntektsKilde mapInntektkilde(FpSak.Beregningsgrunnlag.InntektsKilde inntektsKilde) {
        return switch (inntektsKilde) {
            case INNTEKTSMELDING -> Beregningsgrunnlag.InntektsKilde.INNTEKTSMELDING;
            case A_INNTEKT -> Beregningsgrunnlag.InntektsKilde.A_INNTEKT;
            case SKJØNNSFASTSATT -> Beregningsgrunnlag.InntektsKilde.SKJØNNSFASTSATT;
            case PENSJONSGIVENDE_INNTEKT -> Beregningsgrunnlag.InntektsKilde.PENSJONSGIVENDE_INNTEKT;
            case VEDTAK_ANNEN_YTELSE -> Beregningsgrunnlag.InntektsKilde.VEDTAK_ANNEN_YTELSE;
        };
    }

    private static AktivitetStatus mapAktivitetstatus(FpSak.Beregningsgrunnlag.AktivitetStatus aktivitetStatus) {
        return switch (aktivitetStatus) {
            case ARBEIDSAVKLARINGSPENGER -> AktivitetStatus.ARBEIDSAVKLARINGSPENGER;
            case ARBEIDSTAKER -> AktivitetStatus.ARBEIDSTAKER;
            case DAGPENGER -> AktivitetStatus.DAGPENGER;
            case FRILANSER -> AktivitetStatus.FRILANSER;
            case MILITÆR_ELLER_SIVIL -> AktivitetStatus.MILITÆR_ELLER_SIVIL;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE;
            case KOMBINERT_AT_FL -> AktivitetStatus.KOMBINERT_AT_FL;
            case KOMBINERT_AT_SN -> AktivitetStatus.KOMBINERT_AT_SN;
            case KOMBINERT_FL_SN -> AktivitetStatus.KOMBINERT_FL_SN;
            case KOMBINERT_AT_FL_SN -> AktivitetStatus.KOMBINERT_AT_FL_SN;
            case BRUKERS_ANDEL -> AktivitetStatus.BRUKERS_ANDEL;
            case KUN_YTELSE -> AktivitetStatus.KUN_YTELSE;
        };
    }


    private static List<UttakPeriodeAnnenpartEøs> tilUttaksperiodeAnnenpartEøs(List<FpSak.UttaksperiodeAnnenpartEøs> uttaksperiodeAnnenpartEøs) {
        if (uttaksperiodeAnnenpartEøs == null) {
            return List.of();
        }
        return uttaksperiodeAnnenpartEøs.stream()
            .map(p -> new UttakPeriodeAnnenpartEøs(p.fom(), p.tom(), tilKonto(p.konto()), p.trekkdager()))
            .toList();
    }

    private static List<Uttaksperiode> tilUttaksperiode(List<FpSak.Uttaksperiode> perioder) {
        return Stream.ofNullable(perioder).flatMap(Collection::stream).map(HentSakTask::tilUttaksperiode).toList();
    }

    private static Uttaksperiode tilUttaksperiode(FpSak.Uttaksperiode uttaksperiodeDto) {
        if (uttaksperiodeDto == null) {
            return null;
        }
        var aktiviteter = uttaksperiodeDto.resultat().aktiviteter().stream().map(HentSakTask::tilUttaksperiodeAktivitet).collect(Collectors.toSet());
        return new Uttaksperiode(uttaksperiodeDto.fom(), uttaksperiodeDto.tom(), map(uttaksperiodeDto.utsettelseÅrsak()),
            map(uttaksperiodeDto.oppholdÅrsak()), map(uttaksperiodeDto.overføringÅrsak()), uttaksperiodeDto.samtidigUttak(),
            uttaksperiodeDto.flerbarnsdager(), map(uttaksperiodeDto.morsAktivitet()),
            new Uttaksperiode.Resultat(tilResultatType(uttaksperiodeDto.resultat().type()), map(uttaksperiodeDto.resultat().årsak()), aktiviteter,
                uttaksperiodeDto.resultat().trekkerMinsterett()));
    }

    private static Uttaksperiode.Resultat.Årsak map(FpSak.Uttaksperiode.Resultat.Årsak årsak) {
        return switch (årsak) {
            case AVSLAG_HULL_I_UTTAKSPLAN -> Uttaksperiode.Resultat.Årsak.AVSLAG_HULL_I_UTTAKSPLAN;
            case AVSLAG_UTSETTELSE_TILBAKE_I_TID -> Uttaksperiode.Resultat.Årsak.AVSLAG_UTSETTELSE_TILBAKE_I_TID;
            case INNVILGET_UTTAK_AVSLÅTT_GRADERING_TILBAKE_I_TID -> Uttaksperiode.Resultat.Årsak.INNVILGET_UTTAK_AVSLÅTT_GRADERING_TILBAKE_I_TID;
            case AVSLAG_FRATREKK_PLEIEPENGER -> Uttaksperiode.Resultat.Årsak.AVSLAG_FRATREKK_PLEIEPENGER;
            case ANNET -> Uttaksperiode.Resultat.Årsak.ANNET;
        };
    }

    private static Uttaksperiode.UttaksperiodeAktivitet tilUttaksperiodeAktivitet(FpSak.Uttaksperiode.UttaksperiodeAktivitet a) {
        var type = tilUttakAktivitetType(a);
        var arbeidsgiver = tilArbeidsgiver(a.aktivitet().arbeidsgiver());
        var arbeidsforholdId = a.aktivitet().arbeidsforholdId();
        var trekkdager = a.trekkdager();
        return new Uttaksperiode.UttaksperiodeAktivitet(new UttakAktivitet(type, arbeidsgiver, arbeidsforholdId), tilKonto(a.konto()), trekkdager,
            a.arbeidstidsprosent());
    }

    private static Arbeidsgiver tilArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        return arbeidsgiver == null ? null : new Arbeidsgiver(arbeidsgiver.identifikator());
    }

    private static UttakAktivitet.Type tilUttakAktivitetType(FpSak.Uttaksperiode.UttaksperiodeAktivitet a) {
        return switch (a.aktivitet().type()) {
            case ORDINÆRT_ARBEID -> UttakAktivitet.Type.ORDINÆRT_ARBEID;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> UttakAktivitet.Type.SELVSTENDIG_NÆRINGSDRIVENDE;
            case FRILANS -> UttakAktivitet.Type.FRILANS;
            case ANNET -> UttakAktivitet.Type.ANNET;
        };
    }

    private static Uttaksperiode.Resultat.Type tilResultatType(FpSak.Uttaksperiode.Resultat.Type type) {
        return switch (type) {
            case INNVILGET -> Uttaksperiode.Resultat.Type.INNVILGET;
            case INNVILGET_GRADERING -> Uttaksperiode.Resultat.Type.INNVILGET_GRADERING;
            case AVSLÅTT -> Uttaksperiode.Resultat.Type.AVSLÅTT;
        };
    }

    private static Dekningsgrad tilDekningsgrad(FpSak.Dekningsgrad dekningsgrad) {
        return dekningsgrad == null ? null : switch (dekningsgrad) {
            case HUNDRE -> Dekningsgrad.HUNDRE;
            case ÅTTI -> Dekningsgrad.ÅTTI;
        };
    }
}
