package no.nav.foreldrepenger.oversikt.saker;

import static java.util.function.Predicate.not;
import static no.nav.foreldrepenger.oversikt.saker.SakerDtoMapper.tilDto;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Sak;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;

@ApplicationScoped
public class Saker {

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
        var saker = hentSaker(aktørId);
        return tilDto(saker, fødselsnummerOppslag);
    }

    List<Sak> hentSaker(AktørId aktørId) {
        return sakRepository.hentFor(aktørId).stream()
            .filter(Sak::harSøknad)
            .filter(not(Sak::erHenlagt))
            .toList();
    }
}
