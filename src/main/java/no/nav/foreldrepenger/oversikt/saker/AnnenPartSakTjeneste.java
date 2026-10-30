package no.nav.foreldrepenger.oversikt.saker;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.fp.ForeldrepengerSak;
import no.nav.foreldrepenger.oversikt.domene.fp.FpSøknad;
import no.nav.foreldrepenger.oversikt.uttaksplan.UttaksplanTjeneste;

@ApplicationScoped
public class AnnenPartSakTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(AnnenPartSakTjeneste.class);

    private Saker saker;

    @Inject
    public AnnenPartSakTjeneste(Saker saker) {
        this.saker = saker;
    }

    AnnenPartSakTjeneste() {
        //CDI
    }

    public Optional<ForeldrepengerSak> annenPartGjeldendeSakOppgittSøker(AktørId søker,
                                                                         AktørId annenPart,
                                                                         AktørId barn,
                                                                         LocalDate familiehendelse) {
        var fpSaker = saker.hentSaker(annenPart)
            .stream()
            .filter(ForeldrepengerSak.class::isInstance)
            .map(s -> (ForeldrepengerSak) s)
            .collect(Collectors.toSet());
        if (fpSaker.isEmpty()) {
            LOG.info("Annen part har ingen saker om foreldrepenger");
            return Optional.empty();
        }

        return gjeldendeSak(fpSaker, søker, barn, familiehendelse);
    }

    private Optional<ForeldrepengerSak> gjeldendeSak(Set<ForeldrepengerSak> saker, AktørId søkersAktørId, AktørId barn, LocalDate familiehendelse) {
        var annenPartsSaker = sakerMedAnnenpartLikSøker(søkersAktørId, saker, barn, familiehendelse);

        if (annenPartsSaker.size() > 1) {
            var saksnummer = annenPartsSaker.stream().map(ForeldrepengerSak::saksnummer).collect(Collectors.toSet());
            LOG.info("Fant flere enn 1 sak ved oppslag av annen parts vedtaksperioder."
                + " Velger sak med siste søknad. Saksnummer {}.", saksnummer);
        }
        return annenPartsSaker.stream()
            .max(Comparator.comparing((ForeldrepengerSak sak) -> sak.sisteSøknad().map(FpSøknad::mottattTidspunkt).orElse(null),
                Comparator.nullsFirst(Comparator.naturalOrder())));
    }

    private List<ForeldrepengerSak> sakerMedAnnenpartLikSøker(AktørId søker, Set<ForeldrepengerSak> saker, AktørId barn, LocalDate familiehendelse) {
        return saker
            .stream()
            .filter(sak -> !sak.oppgittAleneomsorg())
            .filter(sak -> sak.annenPartAktørId() != null && sak.annenPartAktørId().equals(søker))
            .filter(sak -> UttaksplanTjeneste.gjelderSammeBarn(barn, familiehendelse, sak))
            .toList();
    }
}
