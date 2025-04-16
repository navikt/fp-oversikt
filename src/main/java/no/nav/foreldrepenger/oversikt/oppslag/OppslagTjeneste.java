package no.nav.foreldrepenger.oversikt.oppslag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.arbeid.EksternArbeidsforholdDto;
import no.nav.foreldrepenger.oversikt.integrasjoner.digdir.KrrSpråkKlient;
import no.nav.foreldrepenger.oversikt.integrasjoner.kontonummer.KontaktInformasjonKlient;
import no.nav.foreldrepenger.oversikt.oppslag.dto.PersonDto;
import no.nav.foreldrepenger.oversikt.oppslag.dto.PersonMedArbeidsforholdDto;
import no.nav.foreldrepenger.oversikt.oppslag.mapper.PersonDtoMapper;
import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;
import no.nav.vedtak.util.LRUCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class OppslagTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(OppslagTjeneste.class);
    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(60, TimeUnit.MINUTES);
    public static final LRUCache<String, PersonDto> PERSONINFO_CACHE = new LRUCache<>(2000, CACHE_ELEMENT_LIVE_TIME_MS);
    public static final LRUCache<String, List<EksternArbeidsforholdDto>> PERSON_ARBEIDSFORHOLD_CACHE = new LRUCache<>(2000, CACHE_ELEMENT_LIVE_TIME_MS);

    private PdlOppslagTjeneste pdlOppslagTjeneste;
    private MineArbeidsforholdTjeneste mineArbeidsforholdTjeneste;
    private KrrSpråkKlient krrSpråkKlient;
    private KontaktInformasjonKlient kontaktInformasjonKlient;
    private InnloggetBruker innloggetBruker;

    public OppslagTjeneste() {
        // CDI
    }

    @Inject
    public OppslagTjeneste(PdlOppslagTjeneste pdlOppslagTjeneste,
                           MineArbeidsforholdTjeneste mineArbeidsforholdTjeneste,
                           KrrSpråkKlient krrSpråkKlient,
                           KontaktInformasjonKlient kontaktInformasjonKlient,
                           InnloggetBruker innloggetBruker) {
        this.pdlOppslagTjeneste = pdlOppslagTjeneste;
        this.mineArbeidsforholdTjeneste = mineArbeidsforholdTjeneste;
        this.krrSpråkKlient = krrSpråkKlient;
        this.kontaktInformasjonKlient = kontaktInformasjonKlient;
        this.innloggetBruker = innloggetBruker;
    }

    public PersonDto personinfoFor() {
        try {
            LOG.info("Henter personinfo for søker");
            var søkersFnr = innloggetBruker.fødselsnummer();
            return Optional.ofNullable(PERSONINFO_CACHE.get(søkersFnr.value()))
                    .orElseGet(() -> hentPersoninfoPåNytt(søkersFnr));
        } catch (Exception e) {
            LOG.info("Feil ved henting av personinfo for søker", e);
            return null;
        }

    }

    public PersonMedArbeidsforholdDto personinfoMedArbeidsforholdFor() {
        try {
            LOG.info("Henter personinfo med arbeidsforhold for søker");
            return new PersonMedArbeidsforholdDto(personinfoFor(), hentEksternArbeidsforhold());
        } catch (Exception e) {
            LOG.info("Feil ved henting av personinfo med arbeidsforhold for søker", e);
            return null;
        }
    }

    private List<EksternArbeidsforholdDto> hentEksternArbeidsforhold() {
        var søkersFnr = innloggetBruker.fødselsnummer();
        return Optional.ofNullable(PERSON_ARBEIDSFORHOLD_CACHE.get(søkersFnr.value()))
                .orElseGet(() -> mineArbeidsforholdTjeneste.brukersArbeidsforhold(søkersFnr));
    }

    private PersonDto hentPersoninfoPåNytt(Fødselsnummer søkersFnr) {
        var søkersAktørid = innloggetBruker.aktørId();
        var søker = pdlOppslagTjeneste.hentSøker(søkersFnr.value());
        var barn = pdlOppslagTjeneste.hentBarnTilSøker(søker);
        var annenpart = pdlOppslagTjeneste.hentAnnenpartRelatertTilBarn(barn, søkersFnr);
        var målform = krrSpråkKlient.finnSpråkkodeForBruker(søkersFnr.value());
        var kontonummer = kontaktInformasjonKlient.hentRegistertKontonummer();
        var personinfoDto = PersonDtoMapper.tilPersonDto(søkersAktørid, søker, barn, annenpart, målform, kontonummer);
        PERSONINFO_CACHE.put(søkersFnr.value(), personinfoDto);
        return personinfoDto;
    }
}