package no.nav.foreldrepenger.oversikt.saker;

import static java.util.function.Predicate.not;
import static no.nav.foreldrepenger.oversikt.saker.SakerDtoMapper.tilDto;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Sak;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;

@ApplicationScoped
public class Saker {

    private SakRepository sakRepository;
    private InnloggetBruker innloggetBruker;
    private PersonOppslagSystem fødselsnummerOppslag;

    @Inject
    public Saker(SakRepository sakRepository, InnloggetBruker innloggetBruker, PersonOppslagSystem fødselsnummerOppslag) {
        this.sakRepository = sakRepository;
        this.innloggetBruker = innloggetBruker;
        this.fødselsnummerOppslag = fødselsnummerOppslag;
    }

    Saker() {
        //CDI
    }

    public no.nav.foreldrepenger.common.innsyn.Saker hent() {
        var saker = hentSaker(innloggetBruker.aktørId());
        return tilDto(saker, fødselsnummerOppslag);
    }

    List<Sak> hentSaker(AktørId aktørId) {
        return sakRepository.hentFor(aktørId).stream()
            .filter(Sak::harSøknad)
            .filter(not(Sak::erUpunchetPapirsøknad))
            .filter(not(Sak::erHenlagt))
            .toList();
    }
}
