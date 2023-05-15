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
import no.nav.foreldrepenger.oversikt.domene.Dekningsgrad;
import no.nav.foreldrepenger.oversikt.domene.Egenskap;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.SakES0;
import no.nav.foreldrepenger.oversikt.domene.SakFP0;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.foreldrepenger.oversikt.domene.SakSVP0;
import no.nav.foreldrepenger.oversikt.domene.SakStatus;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.Uttaksperiode;
import no.nav.foreldrepenger.oversikt.domene.Vedtak;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("hent.sak")
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
        var egenskaper = tilEgenskaper(sakDto.egenskaper());
        if (sakDto instanceof FpSak fpsak) {
            var annenPart = fpsak.oppgittAnnenPart() == null ? null : new AktørId(fpsak.oppgittAnnenPart());
            return new SakFP0(saksnummer, aktørId, status, tilVedtak(fpsak.vedtakene()), annenPart, familieHendelse, aksjonspunkt,
                egenskaper);
        }
        if (sakDto instanceof SvpSak) {
            return new SakSVP0(saksnummer, aktørId, status, familieHendelse, aksjonspunkt, egenskaper);
        }
        if (sakDto instanceof EsSak) {
            return new SakES0(saksnummer, aktørId, status, familieHendelse, aksjonspunkt, egenskaper);
        }

        throw new IllegalStateException("Hentet sak er null og kan ikke bli mappet!");
    }

    private static Set<Egenskap> tilEgenskaper(Set<Sak.Egenskap> egenskaper) {
        return safeStream(egenskaper).map(e -> switch (e) {
            case SØKNAD_UNDER_BEHANDLING -> Egenskap.SØKNAD_UNDER_BEHANDLING;
        }).collect(Collectors.toSet());
    }

    private static Set<Aksjonspunkt> tilAksjonspunkt(Set<Sak.Aksjonspunkt> aksjonspunkt) {
        return safeStream(aksjonspunkt).map(a -> new Aksjonspunkt(a.kode(), switch (a.status()) {
            case UTFØRT -> Aksjonspunkt.Status.UTFØRT;
            case OPPRETTET -> Aksjonspunkt.Status.OPPRETTET;
        }, a.venteÅrsak(), a.opprettetTidspunkt())).collect(Collectors.toSet());
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

    private static Set<Vedtak> tilVedtak(Set<FpSak.Vedtak> vedtakene) {
        return safeStream(vedtakene).map(HentSakTask::tilVedtak).collect(Collectors.toSet());
    }

    private static Vedtak tilVedtak(FpSak.Vedtak vedtakDto) {
        if (vedtakDto == null) {
            return null;
        }
        return new Vedtak(vedtakDto.vedtakstidspunkt(), tilUttaksperiode(vedtakDto.uttaksperioder()), tilDekningsgrad(vedtakDto.dekningsgrad()));
    }

    private static List<Uttaksperiode> tilUttaksperiode(List<FpSak.Uttaksperiode> perioder) {
        return safeStream(perioder).map(HentSakTask::tilUttaksperiode).toList();
    }

    private static Uttaksperiode tilUttaksperiode(FpSak.Uttaksperiode uttaksperiodeDto) {
        if (uttaksperiodeDto == null) {
            return null;
        }
        return new Uttaksperiode(uttaksperiodeDto.fom(), uttaksperiodeDto.tom(),
            new Uttaksperiode.Resultat(tilResultatType(uttaksperiodeDto.resultat().type())));
    }

    private static Uttaksperiode.Resultat.Type tilResultatType(FpSak.Uttaksperiode.Resultat.Type type) {
        return switch (type) {
            case INNVILGET -> Uttaksperiode.Resultat.Type.INNVILGET;
            case AVSLÅTT -> Uttaksperiode.Resultat.Type.AVSLÅTT;
        };
    }

    private static Dekningsgrad tilDekningsgrad(FpSak.Vedtak.Dekningsgrad dekningsgrad) {
        return switch (dekningsgrad) {
            case HUNDRE -> Dekningsgrad.HUNDRE;
            case ÅTTI -> Dekningsgrad.ÅTTI;
        };
    }
}
