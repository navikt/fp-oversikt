package no.nav.foreldrepenger.oversikt.saker;

import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;
import static no.nav.foreldrepenger.oversikt.domene.fp.Uttaksperiode.compress;

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
import no.nav.foreldrepenger.common.innsyn.AnnenPartSak;
import no.nav.foreldrepenger.common.innsyn.Dekningsgrad;
import no.nav.foreldrepenger.common.innsyn.Gradering;
import no.nav.foreldrepenger.common.innsyn.UttakPeriode;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.fp.ForeldrepengerSak;

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

    Optional<AnnenPartSak> hentFor(AktørId søker,
                                      AktørId annenPart,
                                      AktørId barn,
                                      LocalDate familiehendelse) {

        var gjeldendeSakForAnnenPartOpt = annenPartGjeldendeSakOppgittSøker(søker, annenPart, barn, familiehendelse);
        if (gjeldendeSakForAnnenPartOpt.isEmpty()) {
            LOG.info("Finner ingen sak der annen part har oppgitt søker");
            return Optional.empty();
        }
        var gjeldendeSak = gjeldendeSakForAnnenPartOpt.get();
        LOG.info("Fant gjeldende sak for annen part med saksnummer {}", gjeldendeSak.saksnummer().value());
        var termindato = gjeldendeSak.familieHendelse().termindato();
        var antallBarn = gjeldendeSak.familieHendelse().antallBarn();
        var dekningsgrad = switch (gjeldendeSak.dekningsgrad()) {
            case ÅTTI -> Dekningsgrad.ÅTTI;
            case HUNDRE -> Dekningsgrad.HUNDRE;
        };
        var uttakperioder = finnUttaksperioder(gjeldendeSak);
        return Optional.of(new AnnenPartSak(fjernArbeidsgivere(uttakperioder), termindato, dekningsgrad, antallBarn));
    }

    Optional<AnnenPartSak> hentVedtak(AktørId søker,
                                      AktørId annenPart,
                                      AktørId barn,
                                      LocalDate familiehendelse) {
        var gjeldendeSakForAnnenPartOpt = annenPartGjeldendeSakOppgittSøker(søker, annenPart, barn, familiehendelse);

        if (gjeldendeSakForAnnenPartOpt.isEmpty()) {
            LOG.info("Finner ingen sak der annen part har oppgitt søker");
            return Optional.empty();
        }
        var gjeldendeSak = gjeldendeSakForAnnenPartOpt.get();
        LOG.info("Fant gjeldende sak for annen part med saksnummer {}", gjeldendeSak.saksnummer().value());
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
        var vedtaksperioder = safeStream(gjeldendeVedtak.get().perioder()).map(p -> p.tilDto(gjeldendeSak.brukerRolle().tilDto())).toList();
        return Optional.of(new AnnenPartSak(compress(fjernArbeidsgivere(vedtaksperioder)), termindato, dekningsgrad, antallBarn));
    }

    private static List<UttakPeriode> finnUttaksperioder(ForeldrepengerSak gjeldendeSak) {
        //Fra vedtak, ellers søknad
        var brukerRolle = gjeldendeSak.brukerRolle().tilDto();
        return gjeldendeSak.gjeldendeVedtak().map(gjeldendeVedtak -> {
            var perioder = safeStream(gjeldendeVedtak.perioder()).map(p -> p.tilDto(brukerRolle)).toList();
            return compress(perioder);
        }).orElseGet(() -> {
            LOG.info("Annen parts gjeldende sak har ingen gjeldende vedtak. Saksnummer {}", gjeldendeSak.saksnummer());
            return gjeldendeSak.sisteSøknad().map(s -> {
                var perioder = s.perioder().stream().map(fpSøknadsperiode -> fpSøknadsperiode.tilDto(brukerRolle)).toList();
                return compress(perioder);
            }).orElse(List.of());
        });
    }

    private static List<UttakPeriode> fjernArbeidsgivere(List<UttakPeriode> uttakperioder) {
        //SKal ikke kunne se annen parts arbeidsgivere
        return uttakperioder.stream().map(p -> {
            var gradering = p.gradering() == null ? null : new Gradering(p.gradering().arbeidstidprosent(), null);
            return new UttakPeriode(p.fom(), p.tom(),
                p.kontoType(), p.resultat(), p.utsettelseÅrsak(), p.oppholdÅrsak(), p.overføringÅrsak(), gradering,
                p.morsAktivitet(), p.samtidigUttak(), p.flerbarnsdager(), p.forelder());
        }).toList();
    }

    private Optional<ForeldrepengerSak> gjeldendeSak(Set<ForeldrepengerSak> saker, AktørId søkersAktørId, AktørId barn, LocalDate familiehendelse) {
        var annenPartsSaker = sakerMedAnnenpartLikSøker(søkersAktørId, saker, barn, familiehendelse);

        if (annenPartsSaker.size() > 1) {
            var saksnummer = annenPartsSaker.stream().map(ForeldrepengerSak::saksnummer).collect(Collectors.toSet());
            LOG.info("Fant flere enn 1 sak ved oppslag av annen parts vedtaksperioder."
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
            .filter(sak -> matcher(barn, familiehendelse, sak))
            .toList();
    }

    private static boolean matcher(AktørId barn, LocalDate familiehendelse, ForeldrepengerSak sak) {
        if (barn != null) {
            if (sak.gjelderBarn(barn)) {
                return true;
            }
        }
        if (familiehendelse != null && sak.familieHendelse() != null) {
            if (isEquals(familiehendelse, sak.familieHendelse())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEquals(LocalDate dato, FamilieHendelse fh) {
        if (fh.omsorgsovertakelse() != null) return fh.omsorgsovertakelse().equals(dato);
        if (fh.fødselsdato() != null) return fh.fødselsdato().equals(dato);
        return Objects.equals(fh.termindato(), dato);
    }
}

