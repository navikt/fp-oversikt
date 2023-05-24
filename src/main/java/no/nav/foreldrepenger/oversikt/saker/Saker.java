package no.nav.foreldrepenger.oversikt.saker;

import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;
import static no.nav.foreldrepenger.oversikt.saker.SakerDtoMapper.tilDto;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Sak;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;

@ApplicationScoped
public class Saker {

    private static final Logger LOG = LoggerFactory.getLogger(Saker.class);

    private SakRepository sakRepository;
    private FødselsnummerOppslag fødselsnummerOppslag;

    @Inject
    public Saker(SakRepository sakRepository, FødselsnummerOppslag fødselsnummerOppslag) {
        this.sakRepository = sakRepository;
        this.fødselsnummerOppslag = fødselsnummerOppslag;
    }

    Saker() {
        //CDI
    }

    public no.nav.foreldrepenger.common.innsyn.Saker hent(AktørId aktørId) {
        var filtrerteSaker = safeStream(sakRepository.hentFor(aktørId))
            .filter(Sak::harSakSøknad)
            .toList();

        LOG.info("Hentet og filtrerte saker {}", filtrerteSaker);

        return tilDto(filtrerteSaker, fødselsnummerOppslag);
    }
}
