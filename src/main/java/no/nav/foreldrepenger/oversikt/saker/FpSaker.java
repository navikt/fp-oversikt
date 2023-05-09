package no.nav.foreldrepenger.oversikt.saker;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.common.innsyn.EsSak;
import no.nav.foreldrepenger.common.innsyn.FpSak;
import no.nav.foreldrepenger.common.innsyn.Saker;
import no.nav.foreldrepenger.common.innsyn.SvpSak;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Sak;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;

@ApplicationScoped
public class FpSaker {

    private static final Logger LOG = LoggerFactory.getLogger(FpSaker.class);

    private SakRepository sakRepository;
    private FødselsnummerOppslag fødselsnummerOppslag;

    @Inject
    public FpSaker(SakRepository sakRepository, FødselsnummerOppslag fødselsnummerOppslag) {
        this.sakRepository = sakRepository;
        this.fødselsnummerOppslag = fødselsnummerOppslag;
    }

    FpSaker() {
        //CDI
    }

    public Saker hent(AktørId aktørId) {
        var saker = sakRepository.hentFor(aktørId);
        LOG.info("Hentet saker {}", saker);
        return tilDto(saker, fødselsnummerOppslag);
    }

    static Saker tilDto(List<Sak> saker, FødselsnummerOppslag fnrOppslag) {
        var sakerDtoer = saker.stream()
            .map(s -> s.tilSakDto(fnrOppslag))
            .collect(Collectors.toSet());

        var foreldrepenger = sakerDtoer.stream()
            .filter(FpSak.class::isInstance)
            .map(FpSak.class::cast)
            .collect(Collectors.toSet());
        var svangeskapspenger = sakerDtoer.stream()
            .filter(SvpSak.class::isInstance)
            .map(SvpSak.class::cast)
            .collect(Collectors.toSet());
        var engangsstønad = sakerDtoer.stream()
            .filter(EsSak.class::isInstance)
            .map(EsSak.class::cast)
            .collect(Collectors.toSet());
        return new Saker(foreldrepenger, engangsstønad, svangeskapspenger);
    }

}
