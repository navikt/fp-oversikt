package no.nav.foreldrepenger.oversikt.tidslinje;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import no.nav.foreldrepenger.common.domain.Saksnummer;
import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static no.nav.foreldrepenger.oversikt.tidslinje.TidslinjeHendelseDto.TidslinjeHendelseType.ENDRINGSSØKNAD;
import static no.nav.foreldrepenger.oversikt.tidslinje.TidslinjeHendelseDto.TidslinjeHendelseType.ETTERSENDING;
import static no.nav.foreldrepenger.oversikt.tidslinje.TidslinjeHendelseDto.TidslinjeHendelseType.FØRSTEGANGSSØKNAD;
import static no.nav.foreldrepenger.oversikt.tidslinje.TidslinjeHendelseDto.TidslinjeHendelseType.FØRSTEGANGSSØKNAD_NY;
import static no.nav.foreldrepenger.oversikt.tidslinje.TidslinjeHendelseDto.TidslinjeHendelseType.VEDTAK;


@Path("/tidslinje")
@ApplicationScoped
public class TidslinjeRest {
    private static final Logger LOG = LoggerFactory.getLogger(TidslinjeRest.class);

    private TidslinjeTjeneste tidslinjeTjeneste;
    private InnloggetBruker innloggetBruker;

    public TidslinjeRest() {
        // CDI
    }

    @Inject
    public TidslinjeRest(TidslinjeTjeneste tidslinjeTjeneste, InnloggetBruker innloggetBruker) {
        this.tidslinjeTjeneste = tidslinjeTjeneste;
        this.innloggetBruker = innloggetBruker;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<TidslinjeHendelseDto> hentTidslinje(@QueryParam("saksnummer") @Valid @NotNull Saksnummer saksnummer) {
        var tidslinje = tidslinjeTjeneste.tidslinje(innloggetBruker.fødselsnummer(), new no.nav.foreldrepenger.oversikt.domene.Saksnummer(saksnummer.value()));
        tidslinjeKonsistensSjekk(tidslinje);
        return tidslinje;
    }

    private static void tidslinjeKonsistensSjekk(List<TidslinjeHendelseDto> tidslinjeHendelseDto) {
        try {
            for (var innslag : tidslinjeHendelseDto) {
                if (FØRSTEGANGSSØKNAD.equals(innslag.tidslinjeHendelseType())) {
                    if (finnesHendelseTypeTidligereITidslinjen(VEDTAK, innslag, tidslinjeHendelseDto)) {
                        LOG.info("Det finnes vedtak uten førstegangssøknad: {}", tidslinjeHendelseDto);
                    }
                }

                if (FØRSTEGANGSSØKNAD_NY.equals(innslag.tidslinjeHendelseType())) {
                    if (!finnesHendelseTypeTidligereITidslinjen(FØRSTEGANGSSØKNAD, innslag, tidslinjeHendelseDto)) {
                        LOG.info("Det finnes ingen førstegangssøknad før ny førstegangssøknad: {}", tidslinjeHendelseDto);
                    }
                }

                if (ETTERSENDING.equals(innslag.tidslinjeHendelseType())) {
                    if (!finnesHendelseTypeTidligereITidslinjen(FØRSTEGANGSSØKNAD, innslag, tidslinjeHendelseDto)) {
                        LOG.info("Det finnes ikke søknad før ettersendelse: {}", tidslinjeHendelseDto);
                    }
                }

                if (ENDRINGSSØKNAD.equals(innslag.tidslinjeHendelseType())) {
                    if (!finnesHendelseTypeTidligereITidslinjen(FØRSTEGANGSSØKNAD, innslag, tidslinjeHendelseDto) || !finnesHendelseTypeTidligereITidslinjen(VEDTAK, innslag, tidslinjeHendelseDto)) {
                        LOG.info("Det finnes ikke førstegangssøknad og/eller vedtak før endringssøknad: {}", tidslinjeHendelseDto);
                    }
                }
            }
        } catch (Exception e) {
            LOG.info("Konsistens sjekk feilet for tidslinje {}", tidslinjeHendelseDto, e);
        }
    }

    private static boolean finnesHendelseTypeTidligereITidslinjen(TidslinjeHendelseDto.TidslinjeHendelseType tidslinjeHendelseType,
                                                                  TidslinjeHendelseDto hendelse,
                                                                  List<TidslinjeHendelseDto> tidslinjeHendelseDto) {
        return tidslinjeHendelseDto.stream()
            .filter(t -> t.tidslinjeHendelseType().equals(tidslinjeHendelseType))
            .anyMatch(t -> t.opprettet().isBefore(hendelse.opprettet()));
    }
}
