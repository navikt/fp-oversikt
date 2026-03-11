package no.nav.foreldrepenger.oversikt.oppslag.svp;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.integrasjoner.pdl.PdlKlient;
import no.nav.foreldrepenger.oversikt.oppslag.felles.MineArbeidsforholdTjeneste;
import no.nav.foreldrepenger.oversikt.oppslag.felles.PersonMedIdent;
import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;
import no.nav.pdl.FoedselsdatoResponseProjection;
import no.nav.pdl.FolkeregisteridentifikatorResponseProjection;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.KjoennResponseProjection;
import no.nav.pdl.NavnResponseProjection;
import no.nav.pdl.PersonResponseProjection;
import no.nav.vedtak.felles.integrasjon.person.FalskIdentitet;
import no.nav.vedtak.felles.integrasjon.person.PersonMappers;
import no.nav.vedtak.util.LRUCache;

@ApplicationScoped
class SvpPersonopplysningerDtoTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(SvpPersonopplysningerDtoTjeneste.class);
    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(60, TimeUnit.MINUTES);
    private static final LRUCache<String, SvpPersonopplysningerDto> PERSONINFO_CACHE = new LRUCache<>(2000, CACHE_ELEMENT_LIVE_TIME_MS);

    private PdlKlient pdlKlient;
    private MineArbeidsforholdTjeneste mineArbeidsforholdTjeneste;
    private InnloggetBruker innloggetBruker;

    SvpPersonopplysningerDtoTjeneste() {
        // CDI
    }

    @Inject
    SvpPersonopplysningerDtoTjeneste(PdlKlient pdlKlient, MineArbeidsforholdTjeneste mineArbeidsforholdTjeneste, InnloggetBruker innloggetBruker) {
        this.pdlKlient = pdlKlient;
        this.mineArbeidsforholdTjeneste = mineArbeidsforholdTjeneste;
        this.innloggetBruker = innloggetBruker;
    }

    SvpPersonopplysningerDto forInnloggetPerson() {
        LOG.info("Henter svangerskapspenger personinfo for søker");
        var søkersFnr = innloggetBruker.fødselsnummer();
        return Optional.ofNullable(PERSONINFO_CACHE.get(søkersFnr.value()))
            .orElseGet(() -> hentOgCachePersoninfo(søkersFnr));
    }

    private SvpPersonopplysningerDto hentOgCachePersoninfo(Fødselsnummer søkersFnr) {
        var søker = hentSøker(søkersFnr.value());
        var arbeidsforhold = mineArbeidsforholdTjeneste.brukersArbeidsforhold(søkersFnr);
        var personinfoDto = SvpPersonopplysningerDtoMapper.tilDto(søker, arbeidsforhold);
        PERSONINFO_CACHE.put(søkersFnr.value(), personinfoDto);
        return personinfoDto;
    }

    private PersonMedIdent hentSøker(String fnr) {
        var request = new HentPersonQueryRequest();
        request.setIdent(fnr);
        var projection = new PersonResponseProjection().folkeregisteridentifikator(
                new FolkeregisteridentifikatorResponseProjection().identifikasjonsnummer().status())
            .navn(new NavnResponseProjection().fornavn().mellomnavn().etternavn())
            .kjoenn(new KjoennResponseProjection().kjoenn())
            .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato());
        var person = pdlKlient.hentPerson(request, projection);
        if (PersonMappers.manglerIdentifikator(person)) {
            var falskId = FalskIdentitet.finnFalskIdentitet(fnr, pdlKlient).orElse(null);
            return new PersonMedIdent(fnr, person, falskId);
        }
        return new PersonMedIdent(fnr, person);
    }
}

