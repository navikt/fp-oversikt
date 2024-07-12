package no.nav.foreldrepenger.oversikt.saker;

import static java.util.function.Predicate.not;
import static no.nav.foreldrepenger.oversikt.saker.SakerDtoMapper.tilDto;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Sak;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontroll;

@ApplicationScoped
public class Saker {

    private SakRepository sakRepository;
    private InnloggetBruker innloggetBruker;
    private TilgangKontroll tilgangKontroll;

    @Inject
    public Saker(SakRepository sakRepository, InnloggetBruker innloggetBruker, TilgangKontroll tilgangKontroll) {
        this.sakRepository = sakRepository;
        this.innloggetBruker = innloggetBruker;
        this.tilgangKontroll = tilgangKontroll;
    }

    Saker() {
        //CDI
    }

    public no.nav.foreldrepenger.common.innsyn.Saker hent() {
        var saker = hentSaker(innloggetBruker.aktørId());
        return tilDto(saker, tilgangKontroll);
    }

    List<Sak> hentSaker(AktørId aktørId) {
        return sakRepository.hentFor(aktørId).stream()
            .filter(Sak::harSøknad)
            .filter(not(Sak::erKomplettForVisning))
            .filter(not(Sak::erHenlagt))
            .toList();
    }
}
