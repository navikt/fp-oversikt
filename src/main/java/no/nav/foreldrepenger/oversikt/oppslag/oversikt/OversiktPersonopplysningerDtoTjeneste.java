package no.nav.foreldrepenger.oversikt.oppslag.oversikt;

import static no.nav.foreldrepenger.oversikt.oppslag.felles.BarnOgAnnenpartUtil.annenForelderRegisterertPåBarnet;
import static no.nav.foreldrepenger.oversikt.oppslag.felles.BarnOgAnnenpartUtil.barnErYngreEnn40Mnd;
import static no.nav.foreldrepenger.oversikt.oppslag.felles.BarnOgAnnenpartUtil.barnRelatertTil;
import static no.nav.foreldrepenger.oversikt.oppslag.felles.BarnOgAnnenpartUtil.dødfødtBarn;
import static no.nav.foreldrepenger.oversikt.oppslag.felles.BarnOgAnnenpartUtil.harAdressebeskyttelse;
import static no.nav.foreldrepenger.oversikt.oppslag.felles.BarnOgAnnenpartUtil.harDødsdato;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.arbeid.ArbeidsforholdTjeneste;
import no.nav.foreldrepenger.oversikt.integrasjoner.kontonummer.KontoregisterKlient;
import no.nav.foreldrepenger.oversikt.integrasjoner.pdl.PdlKlient;
import no.nav.foreldrepenger.oversikt.integrasjoner.pdl.PdlKlientSystem;
import no.nav.foreldrepenger.oversikt.oppslag.felles.PersonMedIdent;
import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;
import no.nav.pdl.AdressebeskyttelseResponseProjection;
import no.nav.pdl.DoedfoedtBarnResponseProjection;
import no.nav.pdl.DoedsfallResponseProjection;
import no.nav.pdl.FoedselsdatoResponseProjection;
import no.nav.pdl.FolkeregisteridentifikatorResponseProjection;
import no.nav.pdl.ForelderBarnRelasjonResponseProjection;
import no.nav.pdl.HentPersonBolkQueryRequest;
import no.nav.pdl.HentPersonBolkResult;
import no.nav.pdl.HentPersonBolkResultResponseProjection;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.NavnResponseProjection;
import no.nav.pdl.PersonResponseProjection;
import no.nav.vedtak.felles.integrasjon.person.FalskIdentitet;
import no.nav.vedtak.felles.integrasjon.person.PersonMappers;
import no.nav.vedtak.util.LRUCache;

@ApplicationScoped
class OversiktPersonopplysningerDtoTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(OversiktPersonopplysningerDtoTjeneste.class);
    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(60, TimeUnit.MINUTES);
    private static final LRUCache<String, OversiktPersonopplysningerDto> PERSONINFO_CACHE = new LRUCache<>(2000, CACHE_ELEMENT_LIVE_TIME_MS);

    private PdlKlient pdlKlient;
    private PdlKlientSystem pdlKlientSystem;
    private KontoregisterKlient kontoregisterKlient;
    private ArbeidsforholdTjeneste arbeidsforholdTjeneste;
    private InnloggetBruker innloggetBruker;

    OversiktPersonopplysningerDtoTjeneste() {
        // CDI
    }

    @Inject
    public OversiktPersonopplysningerDtoTjeneste(PdlKlientSystem pdlKlientSystem,
                                                 PdlKlient pdlKlient,
                                                 KontoregisterKlient kontoregisterKlient,
                                                 ArbeidsforholdTjeneste arbeidsforholdTjeneste,
                                                 InnloggetBruker innloggetBruker) {
        this.pdlKlientSystem = pdlKlientSystem;
        this.pdlKlient = pdlKlient;
        this.kontoregisterKlient = kontoregisterKlient;
        this.arbeidsforholdTjeneste = arbeidsforholdTjeneste;
        this.innloggetBruker = innloggetBruker;
    }

    public OversiktPersonopplysningerDto forInnloggetPerson() {
        LOG.info("Henter oversikt personinfo for søker");
        var søkersFnr = innloggetBruker.fødselsnummer();
        return Optional.ofNullable(PERSONINFO_CACHE.get(søkersFnr.value())).orElseGet(() -> hentOgCachePersoninfo(søkersFnr));
    }

    private OversiktPersonopplysningerDto hentOgCachePersoninfo(Fødselsnummer søkersFnr) {
        var søker = hentSøker(søkersFnr.value());
        var barn = hentBarnTilSøker(søker);
        var annenpart = hentAnnenpartRelatertTilBarn(barn, søkersFnr);
        var kontonummer = kontoregisterKlient.hentRegistrertKontonummer();
        var harArbeidsforhold = arbeidsforholdTjeneste.harArbeidsforhold(søkersFnr);
        var personinfoDto = OversiktPersonopplysningerDtoMapper.tilDto(søker, barn, annenpart,
            kontonummer.orElse(null), harArbeidsforhold);
        PERSONINFO_CACHE.put(søkersFnr.value(), personinfoDto);
        return personinfoDto;
    }

    private PersonMedIdent hentSøker(String fnr) {
        var request = new HentPersonQueryRequest();
        request.setIdent(fnr);
        var projection = new PersonResponseProjection().folkeregisteridentifikator(
                new FolkeregisteridentifikatorResponseProjection().identifikasjonsnummer().status())
            .navn(new NavnResponseProjection().fornavn().mellomnavn().etternavn())
            .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato())
            .doedfoedtBarn(new DoedfoedtBarnResponseProjection().dato())
            .forelderBarnRelasjon(new ForelderBarnRelasjonResponseProjection().relatertPersonsIdent().relatertPersonsRolle().minRolleForPerson());
        var person = pdlKlient.hentPerson(request, projection);
        if (PersonMappers.manglerIdentifikator(person)) {
            var falskId = FalskIdentitet.finnFalskIdentitet(fnr, pdlKlient).orElse(null);
            return new PersonMedIdent(fnr, person, falskId);
        }
        return new PersonMedIdent(fnr, person);
    }

    private List<PersonMedIdent> hentBarnTilSøker(PersonMedIdent søker) {
        var relaterteBarn = relaterteBarn(søker);
        var dødfødteBarn = Stream.ofNullable(søker.person().getDoedfoedtBarn())
            .flatMap(Collection::stream)
            .filter(d -> d.getDato() != null)
            .map(df -> new PersonMedIdent(null, dødfødtBarn(df)))
            .toList();
        return Stream.concat(relaterteBarn.stream(), dødfødteBarn.stream()).filter(b -> barnErYngreEnn40Mnd(b)).toList();
    }

    private List<PersonMedIdent> relaterteBarn(PersonMedIdent søker) {
        var barnIdenter = barnRelatertTil(søker);
        if (barnIdenter.isEmpty()) {
            return List.of();
        }

        var requestBarn = new HentPersonBolkQueryRequest();
        requestBarn.setIdenter(barnIdenter);
        var projeksjonBolk = new HentPersonBolkResultResponseProjection().ident()
            .person(new PersonResponseProjection().navn(new NavnResponseProjection().fornavn().mellomnavn().etternavn())
                .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato())
                .adressebeskyttelse(new AdressebeskyttelseResponseProjection().gradering())
                .doedsfall(new DoedsfallResponseProjection().doedsdato())
                .forelderBarnRelasjon(
                    new ForelderBarnRelasjonResponseProjection().relatertPersonsIdent().relatertPersonsRolle().minRolleForPerson()));
        return pdlKlientSystem.hentPersonBolk(requestBarn, projeksjonBolk)
            .stream()
            .collect(Collectors.toMap(HentPersonBolkResult::getIdent, person -> person, (existing, _) -> existing))
            .values()
            .stream()
            .filter(b -> !harAdressebeskyttelse(b.getPerson()))
            .map(p -> new PersonMedIdent(p.getIdent(), p.getPerson()))
            .toList();
    }

    private Map<String, PersonMedIdent> hentAnnenpartRelatertTilBarn(List<PersonMedIdent> barn, Fødselsnummer søkersFnr) {
        if (barn.isEmpty()) {
            return Map.of();
        }

        var barnTilAnnenpartMapping = barn.stream()
            .filter(barnet -> barnet.ident() != null)
            .map(barnet -> Map.entry(barnet.ident(), annenForelderRegisterertPåBarnet(søkersFnr, barnet)))
            .filter(entry -> entry.getValue().isPresent())
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));

        var annenpartIDenter = barnTilAnnenpartMapping.values().stream().distinct().toList();
        if (annenpartIDenter.isEmpty()) {
            return Map.of();
        }

        var request = new HentPersonBolkQueryRequest();
        request.setIdenter(annenpartIDenter);
        var projeksjonAnnenpart = new HentPersonBolkResultResponseProjection().ident()
            .person(new PersonResponseProjection().navn(new NavnResponseProjection().fornavn().mellomnavn().etternavn())
                .doedsfall(new DoedsfallResponseProjection().doedsdato())
                .adressebeskyttelse(new AdressebeskyttelseResponseProjection().gradering()));

        var annenpartOppslag = pdlKlientSystem.hentPersonBolk(request, projeksjonAnnenpart)
            .stream()
            .filter(result -> !harAdressebeskyttelse(result.getPerson()))
            .filter(result -> !harDødsdato(result.getPerson()))
            .map(result -> new PersonMedIdent(result.getIdent(), result.getPerson()))
            .collect(Collectors.toMap(PersonMedIdent::ident, person -> person));

        if (annenpartOppslag.isEmpty()) {
            return Map.of();
        }

        return barnTilAnnenpartMapping.entrySet()
            .stream()
            .filter(entry -> annenpartOppslag.containsKey(entry.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> annenpartOppslag.get(entry.getValue())));
    }
}
