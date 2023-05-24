package no.nav.foreldrepenger.oversikt.innhenting;

import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.oversikt.domene.Aksjonspunkt;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;
import no.nav.foreldrepenger.oversikt.domene.BrukerRolle;
import no.nav.foreldrepenger.oversikt.domene.Dekningsgrad;
import no.nav.foreldrepenger.oversikt.domene.EsSøknad;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.FpSøknad;
import no.nav.foreldrepenger.oversikt.domene.FpSøknadsperiode;
import no.nav.foreldrepenger.oversikt.domene.FpVedtak;
import no.nav.foreldrepenger.oversikt.domene.Gradering;
import no.nav.foreldrepenger.oversikt.domene.Konto;
import no.nav.foreldrepenger.oversikt.domene.MorsAktivitet;
import no.nav.foreldrepenger.oversikt.domene.OppholdÅrsak;
import no.nav.foreldrepenger.oversikt.domene.OverføringÅrsak;
import no.nav.foreldrepenger.oversikt.domene.Rettigheter;
import no.nav.foreldrepenger.oversikt.domene.SakES0;
import no.nav.foreldrepenger.oversikt.domene.SakFP0;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.foreldrepenger.oversikt.domene.SakSVP0;
import no.nav.foreldrepenger.oversikt.domene.SakStatus;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.SvpSøknad;
import no.nav.foreldrepenger.oversikt.domene.SøknadStatus;
import no.nav.foreldrepenger.oversikt.domene.UtsettelseÅrsak;
import no.nav.foreldrepenger.oversikt.domene.UttakAktivitet;
import no.nav.foreldrepenger.oversikt.domene.Uttaksperiode;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "hent.sak", thenDelay = 600, maxFailedRuns = 20)
public class HentSakTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HentSakTask.class);
    static final String SAKSNUMMER = "saksnummer";

    private final FpsakTjeneste fpSakKlient;
    private final SakRepository sakRepository;

    @Inject
    public HentSakTask(FpsakTjeneste fpsakTjeneste, SakRepository sakRepository) {
        this.fpSakKlient = fpsakTjeneste;
        this.sakRepository = sakRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        LOG.info("kjører task");
        hentOgLagreSak(fpSakKlient, sakRepository, new Saksnummer(prosessTaskData.getPropertyValue(SAKSNUMMER)));
    }

    public static void hentOgLagreSak(FpsakTjeneste fpsak, SakRepository repository, Saksnummer saksnummer) {
        var sakDto = fpsak.hentSak(saksnummer);
        LOG.info("Hentet sak {} {}", saksnummer, sakDto);

        repository.lagre(map(sakDto));
    }

    static no.nav.foreldrepenger.oversikt.domene.Sak map(Sak sakDto) {
        if (sakDto == null) {
            throw new IllegalStateException("Hentet sak er null og kan ikke bli mappet!");
        }

        var familieHendelse = tilFamilieHendelse(sakDto.familieHendelse());
        var status = tilStatus(sakDto.status());
        var saksnummer = new Saksnummer(sakDto.saksnummer());
        var aktørId = new AktørId(sakDto.aktørId());
        var aksjonspunkt = tilAksjonspunkt(sakDto.aksjonspunkt());
        if (sakDto instanceof FpSak fpsak) {
            var annenPart = fpsak.oppgittAnnenPart() == null ? null : new AktørId(fpsak.oppgittAnnenPart());
            var søknader = fpsak.søknader().stream().map(HentSakTask::tilFpSøknad).collect(Collectors.toSet());
            var brukerRolle = tilBrukerRolle(fpsak.brukerRolle());
            var fødteBarn = tilFødteBarn(fpsak.fødteBarn());
            return new SakFP0(saksnummer, aktørId, status, tilVedtak(fpsak.vedtak()), annenPart, familieHendelse, aksjonspunkt, søknader,
                brukerRolle, fødteBarn, tilRettigheter(fpsak.rettigheter()), fpsak.ønskerJustertUttakVedFødsel());
        }
        if (sakDto instanceof SvpSak svpSak) {
            var søknader = svpSak.søknader().stream().map(HentSakTask::tilSvpSøknad).collect(Collectors.toSet());
            return new SakSVP0(saksnummer, aktørId, status, familieHendelse, aksjonspunkt, søknader);
        }
        if (sakDto instanceof EsSak esSak) {
            var søknader = esSak.søknader().stream().map(HentSakTask::tilEsSøknad).collect(Collectors.toSet());
            return new SakES0(saksnummer, aktørId, status, familieHendelse, aksjonspunkt, søknader);
        }

        throw new IllegalStateException("Hentet sak er null og kan ikke bli mappet!");
    }

    private static Rettigheter tilRettigheter(FpSak.Rettigheter rettigheter) {
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
        return new SvpSøknad(map(søknad.status()), søknad.mottattTidspunkt());
    }

    private static FpSøknad tilFpSøknad(FpSak.Søknad søknad) {
        var perioder = søknad.perioder().stream().map(HentSakTask::tilSøknadsperiode).collect(Collectors.toSet());
        return new FpSøknad(map(søknad.status()), søknad.mottattTidspunkt(), perioder, tilDekningsgrad(søknad.dekningsgrad()));
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
        if (konto == null) return null;
        return switch (konto) {
            case FORELDREPENGER -> Konto.FORELDREPENGER;
            case MØDREKVOTE -> Konto.MØDREKVOTE;
            case FEDREKVOTE -> Konto.FEDREKVOTE;
            case FELLESPERIODE -> Konto.FELLESPERIODE;
            case FORELDREPENGER_FØR_FØDSEL -> Konto.FORELDREPENGER_FØR_FØDSEL;
        };
    }

    private static Set<Aksjonspunkt> tilAksjonspunkt(Set<Sak.Aksjonspunkt> aksjonspunkt) {
        return safeStream(aksjonspunkt).map(a -> {
            var venteårsak = a.venteårsak() == null ? null : switch (a.venteårsak()) {
                case ANKE_VENTER_PÅ_MERKNADER_FRA_BRUKER -> Aksjonspunkt.Venteårsak.ANKE_VENTER_PÅ_MERKNADER_FRA_BRUKER;
                case AVVENT_DOKUMTANSJON -> Aksjonspunkt.Venteårsak.AVVENT_DOKUMTANSJON;
                case AVVENT_FØDSEL -> Aksjonspunkt.Venteårsak.AVVENT_FØDSEL;
                case AVVENT_RESPONS_REVURDERING -> Aksjonspunkt.Venteårsak.AVVENT_RESPONS_REVURDERING;
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
            };
            return new Aksjonspunkt(type, venteårsak, a.tidsfrist());
        }).collect(Collectors.toSet());
    }

    private static SakStatus tilStatus(Sak.Status status) {
        return switch (status) {
            case OPPRETTET -> SakStatus.OPPRETTET;
            case UNDER_BEHANDLING -> SakStatus.UNDER_BEHANDLING;
            case LØPENDE -> SakStatus.LØPENDE;
            case AVSLUTTET -> SakStatus.AVSLUTTET;
        };
    }

    private static FamilieHendelse tilFamilieHendelse(Sak.FamilieHendelse familiehendelse) {
        return familiehendelse == null ? null : new FamilieHendelse(familiehendelse.fødselsdato(), familiehendelse.termindato(),
            familiehendelse.antallBarn(), familiehendelse.omsorgsovertakelse());
    }

    private static Set<FpVedtak> tilVedtak(Set<FpSak.Vedtak> vedtakene) {
        return safeStream(vedtakene).map(HentSakTask::tilVedtak).collect(Collectors.toSet());
    }

    private static FpVedtak tilVedtak(FpSak.Vedtak vedtakDto) {
        if (vedtakDto == null) {
            return null;
        }
        return new FpVedtak(vedtakDto.vedtakstidspunkt(), tilUttaksperiode(vedtakDto.uttaksperioder()), tilDekningsgrad(vedtakDto.dekningsgrad()));
    }

    private static List<Uttaksperiode> tilUttaksperiode(List<FpSak.Uttaksperiode> perioder) {
        return safeStream(perioder).map(HentSakTask::tilUttaksperiode).toList();
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
            case ANNET -> Uttaksperiode.Resultat.Årsak.ANNET;
        };
    }

    private static Uttaksperiode.UttaksperiodeAktivitet tilUttaksperiodeAktivitet(FpSak.Uttaksperiode.UttaksperiodeAktivitet a) {
        var type = tilUttakAktivitetType(a);
        var arbeidsgiver = a.aktivitet().arbeidsgiver() == null ? null : new Arbeidsgiver(a.aktivitet().arbeidsgiver().identifikator());
        var arbeidsforholdId = a.aktivitet().arbeidsforholdId();
        var trekkdager = a.trekkdager();
        return new Uttaksperiode.UttaksperiodeAktivitet(new UttakAktivitet(type, arbeidsgiver, arbeidsforholdId), tilKonto(a.konto()), trekkdager,
            a.arbeidstidsprosent());
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
        return switch (dekningsgrad) {
            case HUNDRE -> Dekningsgrad.HUNDRE;
            case ÅTTI -> Dekningsgrad.ÅTTI;
        };
    }
}
