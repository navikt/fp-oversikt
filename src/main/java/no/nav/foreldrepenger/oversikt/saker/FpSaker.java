package no.nav.foreldrepenger.oversikt.saker;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.common.innsyn.BehandlingTilstand;
import no.nav.foreldrepenger.common.innsyn.Dekningsgrad;
import no.nav.foreldrepenger.common.innsyn.Familiehendelse;
import no.nav.foreldrepenger.common.innsyn.FpSak;
import no.nav.foreldrepenger.common.innsyn.FpVedtak;
import no.nav.foreldrepenger.common.innsyn.FpÅpenBehandling;
import no.nav.foreldrepenger.common.innsyn.KontoType;
import no.nav.foreldrepenger.common.innsyn.RettighetType;
import no.nav.foreldrepenger.common.innsyn.Saker;
import no.nav.foreldrepenger.common.innsyn.Saksnummer;
import no.nav.foreldrepenger.common.innsyn.UttakPeriode;
import no.nav.foreldrepenger.common.innsyn.UttakPeriodeResultat;
import no.nav.foreldrepenger.oversikt.Vedtak;
import no.nav.foreldrepenger.oversikt.VedtakRepository;

@ApplicationScoped
public class FpSaker {

    private static final Logger LOG = LoggerFactory.getLogger(FpSaker.class);

    private VedtakRepository vedtakRepository;

    @Inject
    public FpSaker(VedtakRepository vedtakRepository) {
        this.vedtakRepository = vedtakRepository;
    }

    FpSaker() {
        //CDI
    }

    public Saker hent(String aktørId) {
        var vedtak = this.vedtakRepository.hentFor(aktørId);
        LOG.info("Hentet vedtak {}", vedtak);
        return tilDto(vedtak);
    }

    private static Saker tilDto(List<Vedtak> vedtak) {
        var foreldrepenger = vedtak.stream().map(v -> {
            var uttakPeriode = new UttakPeriode(LocalDate.now(), LocalDate.now().plusWeeks(1), KontoType.MØDREKVOTE,
                new UttakPeriodeResultat(true, false, true, UttakPeriodeResultat.Årsak.ANNET), null, null, null, null, null, null, false);
            return new FpSak(new Saksnummer(v.saksnummer()), false, LocalDate.now(), false, true, false, false, false, false,
                RettighetType.BEGGE_RETT, null, new Familiehendelse(null, LocalDate.now(), 1, null), new FpVedtak(List.of(uttakPeriode)),
                new FpÅpenBehandling(BehandlingTilstand.VENT_DOKUMENTASJON, List.of(uttakPeriode)), Set.of(), Dekningsgrad.HUNDRE);
        }).collect(Collectors.toSet());
        return new Saker(foreldrepenger, Set.of(), Set.of());
    }
}
