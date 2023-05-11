package no.nav.foreldrepenger.oversikt.innhenting;

import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Dekningsgrad;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.SakES0;
import no.nav.foreldrepenger.oversikt.domene.SakFP0;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.foreldrepenger.oversikt.domene.SakSVP0;
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
    static final String BEHANDLING_UUID = "behandlingUuid";

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
        hentOgLagreSak(fpSakKlient, sakRepository, UUID.fromString(prosessTaskData.getPropertyValue(BEHANDLING_UUID)));
    }

    public static void hentOgLagreSak(FpsakTjeneste fpsak, SakRepository repository, UUID behandlingUuid) {
        var sakDto = fpsak.hentSak(behandlingUuid);
        LOG.info("Hentet sak {} {}", behandlingUuid, sakDto);

        repository.lagre(map(sakDto));
    }

    static no.nav.foreldrepenger.oversikt.domene.Sak map(Sak sakDto) {
        if (sakDto == null) {
            throw new IllegalStateException("Hentet sak er null og kan ikke bli mappet!");
        }

        if (sakDto instanceof FpSak fpsak) {
            return new SakFP0(new Saksnummer(fpsak.saksnummer()), new AktørId(fpsak.aktørId()), tilVedtak(fpsak.vedtakene()),
                fpsak.oppgittAnnenPart() == null ? null : new AktørId(fpsak.oppgittAnnenPart()), tilFamilieHendelse(fpsak.familieHendelse()));
        }
        if (sakDto instanceof SvpSak svpSak) {
            return new SakSVP0(new Saksnummer(svpSak.saksnummer()), new AktørId(svpSak.aktørId()), tilFamilieHendelse(svpSak.familieHendelse()));
        }
        if (sakDto instanceof EsSak esSak) {
            return new SakES0(new Saksnummer(esSak.saksnummer()), new AktørId(esSak.aktørId()), tilFamilieHendelse(esSak.familieHendelse()));
        }

        throw new IllegalStateException("Hentet sak er null og kan ikke bli mappet!");
    }

    private static FamilieHendelse tilFamilieHendelse(Sak.FamilieHendelse familiehendelse) {
        return familiehendelse == null ? null : new FamilieHendelse(familiehendelse.fødselsdato(), familiehendelse.termindato(),
                familiehendelse.antallBarn(), familiehendelse.omsorgsovertakelse());
    }

    private static Set<Vedtak> tilVedtak(Set<FpSak.Vedtak> vedtakene) {
        return safeStream(vedtakene)
            .map(HentSakTask::tilVedtak)
            .collect(Collectors.toSet());
    }

    private static Vedtak tilVedtak(FpSak.Vedtak vedtakDto) {
        if (vedtakDto == null) {
            return null;
        }
        return new Vedtak(vedtakDto.vedtakstidspunkt(), tilUttaksperiode(vedtakDto.uttaksperioder()), tilDekningsgrad(vedtakDto.dekningsgrad()));
    }

    private static List<Uttaksperiode> tilUttaksperiode(List<FpSak.Uttaksperiode> perioder) {
        return safeStream(perioder)
            .map(HentSakTask::tilUttaksperiode)
            .toList();
    }

    private static Uttaksperiode tilUttaksperiode(FpSak.Uttaksperiode uttaksperiodeDto) {
        if (uttaksperiodeDto == null) {
            return null;
        }
        return new Uttaksperiode(uttaksperiodeDto.fom(), uttaksperiodeDto.tom(), new Uttaksperiode.Resultat(tilResultatType(uttaksperiodeDto.resultat().type())));
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
