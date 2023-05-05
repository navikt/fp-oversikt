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
import no.nav.foreldrepenger.oversikt.domene.Sak;
import no.nav.foreldrepenger.oversikt.domene.SakFP0;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.Uttak;
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
        var behandlingUuid = UUID.fromString(prosessTaskData.getPropertyValue(BEHANDLING_UUID));
        var sakDto = fpSakKlient.hentSak(behandlingUuid);
        LOG.info("Hentet sak {} {}", behandlingUuid, sakDto);

        sakRepository.lagre(map(sakDto));
    }

    static Sak map(FpsakTjeneste.SakDto sakDto) {
        return new SakFP0(new Saksnummer(sakDto.saksnummer()), new AktørId(sakDto.aktørId()), tilVedtak(sakDto.vedtakene()));
    }

    private static Set<Vedtak> tilVedtak(Set<FpsakTjeneste.SakDto.VedtakDto> vedtakene) {
        return safeStream(vedtakene)
            .map(HentSakTask::tilVedtak)
            .collect(Collectors.toSet());
    }

    private static Vedtak tilVedtak(FpsakTjeneste.SakDto.VedtakDto vedtakDto) {
        if (vedtakDto == null) {
            return null;
        }
        return new Vedtak(vedtakDto.vedtakstidspunkt(), tilUttak(vedtakDto.uttak()));
    }

    private static Uttak tilUttak(FpsakTjeneste.SakDto.UttakDto uttakDto) {
        if (uttakDto == null) {
            return null;
        }
        return new Uttak(tilDekningsgrad(uttakDto.dekningsgrad()), tilUttaksperiode(uttakDto.perioder()));
    }

    private static List<Uttaksperiode> tilUttaksperiode(List<FpsakTjeneste.SakDto.UttaksperiodeDto> perioder) {
        return safeStream(perioder)
            .map(HentSakTask::tilUttaksperiode)
            .toList();
    }

    private static Uttaksperiode tilUttaksperiode(FpsakTjeneste.SakDto.UttaksperiodeDto uttaksperiodeDto) {
        if (uttaksperiodeDto == null) {
            return null;
        }
        return new Uttaksperiode(uttaksperiodeDto.fom(), uttaksperiodeDto.tom());
    }

    private static Dekningsgrad tilDekningsgrad(no.nav.foreldrepenger.common.innsyn.Dekningsgrad dekningsgrad) {
        return switch (dekningsgrad) {
            case HUNDRE -> Dekningsgrad.HUNDRE;
            case ÅTTI -> Dekningsgrad.ÅTTI;
        };
    }
}
