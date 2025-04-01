package no.nav.foreldrepenger.oversikt.oppslag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.integrasjoner.aareg.EksternArbeidsforhold;
import no.nav.foreldrepenger.oversikt.integrasjoner.aareg.MineArbeidsforholdTjeneste;
import no.nav.foreldrepenger.oversikt.integrasjoner.digdir.KrrSpråkKlient;
import no.nav.foreldrepenger.oversikt.integrasjoner.kontonummer.KontaktInformasjonKlient;
import no.nav.foreldrepenger.oversikt.oppslag.dto.PersonDto;
import no.nav.foreldrepenger.oversikt.oppslag.dto.PersonMedArbeidsforholdDto;
import no.nav.foreldrepenger.oversikt.oppslag.mapper.PersonDtoMapper;
import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;
import no.nav.vedtak.util.LRUCache;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static no.nav.foreldrepenger.oversikt.oppslag.mapper.ArbeidsforholdDtoMapper.tilArbeidsforholdDto;

@Path("/person")
@ApplicationScoped
@Transactional
public class OppslagRestTjeneste {

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(60, TimeUnit.MINUTES);
    public static final LRUCache<String, PersonDto> PERSONINFO_CACHE = new LRUCache<>(2000, CACHE_ELEMENT_LIVE_TIME_MS);
    public static final LRUCache<String, List<EksternArbeidsforhold>> PERSON_ARBEIDSFORHOLD_CACHE = new LRUCache<>(2000, CACHE_ELEMENT_LIVE_TIME_MS);

    private PdlOppslagTjeneste pdlOppslagTjeneste;
    private MineArbeidsforholdTjeneste mineArbeidsforholdTjeneste;
    private KrrSpråkKlient krrSpråkKlient;
    private KontaktInformasjonKlient kontaktInformasjonKlient;
    private TilgangKontrollTjeneste tilgangkontroll;
    private InnloggetBruker innloggetBruker;

    public OppslagRestTjeneste() {
        // CDI
    }

    @Inject
    public OppslagRestTjeneste(PdlOppslagTjeneste pdlOppslagTjeneste,
                               MineArbeidsforholdTjeneste mineArbeidsforholdTjeneste,
                               KrrSpråkKlient krrSpråkKlient,
                               KontaktInformasjonKlient kontaktInformasjonKlient,
                               TilgangKontrollTjeneste tilgangkontroll,
                               InnloggetBruker innloggetBruker) {
        this.pdlOppslagTjeneste = pdlOppslagTjeneste;
        this.mineArbeidsforholdTjeneste = mineArbeidsforholdTjeneste;
        this.krrSpråkKlient = krrSpråkKlient;
        this.kontaktInformasjonKlient = kontaktInformasjonKlient;
        this.tilgangkontroll = tilgangkontroll;
        this.innloggetBruker = innloggetBruker;
    }

    @Path("/info")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public PersonDto personinfo() {
        tilgangkontroll.sjekkAtKallErFraBorger();
        tilgangkontroll.tilgangssjekkMyndighetsalder();
        var søkersFnr = innloggetBruker.fødselsnummer();
        return hentPersoninfoFor(søkersFnr.value());
    }

    @Path("/info-med-arbeidsforhold")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public PersonMedArbeidsforholdDto søkerinfo() {
        tilgangkontroll.sjekkAtKallErFraBorger();
        tilgangkontroll.tilgangssjekkMyndighetsalder();
        var søkersFnr = innloggetBruker.fødselsnummer();
        var personinfo = hentPersoninfoFor(søkersFnr.value());
        var arbeidsforhold = tilArbeidsforholdDto(hentEksternArbeidsforhold(søkersFnr));
        return new PersonMedArbeidsforholdDto(personinfo, arbeidsforhold);
    }

    private List<EksternArbeidsforhold> hentEksternArbeidsforhold(Fødselsnummer søkersFnr) {
        return Optional.ofNullable(PERSON_ARBEIDSFORHOLD_CACHE.get(søkersFnr.value()))
                .orElseGet(() -> mineArbeidsforholdTjeneste.brukersArbeidsforhold(søkersFnr));
    }

    private PersonDto hentPersoninfoFor(String søkersFnr) {
        return Optional.ofNullable(PERSONINFO_CACHE.get(søkersFnr))
                .orElseGet(() -> hentPersoninfoPåNytt(søkersFnr));
    }

    private PersonDto hentPersoninfoPåNytt(String søkersFnr) {
        var aktøridSøker = innloggetBruker.aktørId();
        var søker = pdlOppslagTjeneste.hentSøker(søkersFnr);
        var barn = pdlOppslagTjeneste.hentBarnTilSøker(søker);
        var annenpart = pdlOppslagTjeneste.hentAnnenpartRelatertTilBarn(barn, søker);
        var målform = krrSpråkKlient.finnSpråkkodeForBruker(søkersFnr);
        var kontonummer = kontaktInformasjonKlient.hentRegistertKontonummer();
        var personinfoDto = PersonDtoMapper.tilPersonDto(aktøridSøker, søker, barn, annenpart, målform, kontonummer);
        PERSONINFO_CACHE.put(søkersFnr, personinfoDto);
        return personinfoDto;
    }
}