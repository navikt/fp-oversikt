package no.nav.foreldrepenger.oversikt.saker;

import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.common.innsyn.AnnenPartVedtak;
import no.nav.foreldrepenger.common.innsyn.Dekningsgrad;
import no.nav.foreldrepenger.common.innsyn.Gradering;
import no.nav.foreldrepenger.common.innsyn.UttakPeriode;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.fp.ForeldrepengerSak;
import no.nav.foreldrepenger.oversikt.domene.fp.FpVedtak;
import no.nav.foreldrepenger.oversikt.domene.fp.Uttaksperiode;

@ApplicationScoped
public class AnnenPartVedtakTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(AnnenPartVedtakTjeneste.class);

    private Saker saker;

    @Inject
    public AnnenPartVedtakTjeneste(Saker saker) {
        this.saker = saker;
    }

    AnnenPartVedtakTjeneste() {
        //CDI
    }

    Optional<AnnenPartVedtak> hentFor(AktørId søker,
                                      AktørId annenPart,
                                      AktørId barn,
                                      LocalDate familiehendelse) {
        var fpSaker = saker.hentSaker(annenPart).stream()
            .filter(ForeldrepengerSak.class::isInstance)
            .map(s -> (ForeldrepengerSak) s)
            .collect(Collectors.toSet());
        if (fpSaker.isEmpty()) {
            LOG.info("Annen part har ingen saker om foreldrepenger");
            return Optional.empty();
        }

        var gjeldendeSakForAnnenPartOpt = gjeldendeSak(fpSaker, søker, barn, familiehendelse);
        if (gjeldendeSakForAnnenPartOpt.isEmpty()) {
            LOG.info("Finner ingen sak som ikke er avsluttet der annen part har oppgitt søker");
            return Optional.empty();
        }
        var gjeldendeSak = gjeldendeSakForAnnenPartOpt.get();
        LOG.info("Fant gjeldende sak for annen part. Saksnummer {}", gjeldendeSak.saksnummer());
        var gjeldendeVedtak = gjeldendeSak.gjeldendeVedtak();
        if (gjeldendeVedtak.isEmpty()) {
            LOG.info("Annen parts gjeldende sak har ingen gjeldende vedtak. Saksnummer {}", gjeldendeSak.saksnummer());
            return Optional.empty();
        }
        var termindato = gjeldendeSak.familieHendelse().termindato();
        var antallBarn = gjeldendeSak.familieHendelse().antallBarn();
        var dekningsgrad = switch (gjeldendeSak.dekningsgrad()) {
            case ÅTTI -> Dekningsgrad.ÅTTI;
            case HUNDRE -> Dekningsgrad.HUNDRE;
        };
        return Optional.of(new AnnenPartVedtak(filterSensitive(gjeldendeVedtak.get()), termindato, dekningsgrad, antallBarn));
    }

    private static List<UttakPeriode> filterSensitive(FpVedtak gjeldendeVedtak) {
        //SKal ikke kunne se annen parts arbeidsgivere
        return safeStream(gjeldendeVedtak.perioder()).map(Uttaksperiode::tilDto).map(p -> {
            var gradering = p.gradering() == null ? null : new Gradering(p.gradering().arbeidstidprosent(), null);
            return new UttakPeriode(p.fom(), p.tom(),
                p.kontoType(), p.resultat(), p.utsettelseÅrsak(), p.oppholdÅrsak(), p.overføringÅrsak(), gradering,
                p.morsAktivitet(), p.samtidigUttak(), p.flerbarnsdager());
        }).toList();
    }

    private Optional<ForeldrepengerSak> gjeldendeSak(Set<ForeldrepengerSak> saker, AktørId søkersAktørId, AktørId barn, LocalDate familiehendelse) {
        var annenPartsSaker = sakerMedAnnenpartLikSøker(søkersAktørId, saker, barn, familiehendelse);

        if (annenPartsSaker.size() > 1) {
            var saksnummer = annenPartsSaker.stream().map(ForeldrepengerSak::saksnummer).collect(Collectors.toSet());
            LOG.warn("Fant flere enn 1 sak ved oppslag av annen parts vedtaksperioder."
                + " Velger sak med siste søknad. Saksnummer {}.", saksnummer);
        }
        return annenPartsSaker.stream()
            .max((o1, o2) -> {
                var mt2 = o2.sisteSøknad().orElseThrow().mottattTidspunkt();
                var mt1 = o1.sisteSøknad().orElseThrow().mottattTidspunkt();
                return mt2.compareTo(mt1);
            });
    }

    private List<ForeldrepengerSak> sakerMedAnnenpartLikSøker(AktørId søker, Set<ForeldrepengerSak> saker, AktørId barn, LocalDate familiehendelse) {
        return saker
            .stream()
            .filter(sak -> !sak.oppgittAleneomsorg())
            .filter(sak -> sak.annenPartAktørId() != null && sak.annenPartAktørId().equals(søker))
            .filter(sak -> barn == null || sak.gjelderBarn(barn))
            //Sjekker ikke familiehendelse hvis vi har aktørId på barnet
            .filter(sak -> barn != null || familiehendelse == null || isEquals(familiehendelse, sak.familieHendelse()))
            .toList();
    }

    private static boolean isEquals(LocalDate dato, FamilieHendelse fh) {
        if (fh.fødselsdato() != null) return fh.fødselsdato().equals(dato);
        if (fh.termindato() != null) return fh.termindato().equals(dato);
        return Objects.equals(fh.omsorgsovertakelse(), dato);
    }
}

