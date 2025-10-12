package no.nav.foreldrepenger.oversikt.oppslag;

import static no.nav.foreldrepenger.common.util.StreamUtil.safeStream;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.integrasjoner.pdl.PdlKlient;
import no.nav.foreldrepenger.oversikt.integrasjoner.pdl.PdlKlientSystem;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.AdresseBeskyttelse;
import no.nav.pdl.Adressebeskyttelse;
import no.nav.pdl.AdressebeskyttelseResponseProjection;
import no.nav.pdl.DoedfoedtBarn;
import no.nav.pdl.DoedfoedtBarnResponseProjection;
import no.nav.pdl.Doedsfall;
import no.nav.pdl.DoedsfallResponseProjection;
import no.nav.pdl.Foedselsdato;
import no.nav.pdl.FoedselsdatoResponseProjection;
import no.nav.pdl.FolkeregisteridentifikatorResponseProjection;
import no.nav.pdl.ForelderBarnRelasjon;
import no.nav.pdl.ForelderBarnRelasjonResponseProjection;
import no.nav.pdl.ForelderBarnRelasjonRolle;
import no.nav.pdl.HentPersonBolkQueryRequest;
import no.nav.pdl.HentPersonBolkResultResponseProjection;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.KjoennResponseProjection;
import no.nav.pdl.NavnResponseProjection;
import no.nav.pdl.Person;
import no.nav.pdl.PersonResponseProjection;
import no.nav.pdl.SivilstandResponseProjection;
import no.nav.vedtak.felles.integrasjon.person.FalskIdentitet;
import no.nav.vedtak.felles.integrasjon.person.PersonMappers;

@ApplicationScoped
public class PdlOppslagTjeneste {
    private static final int IKKE_ELDRE_ENN_40_MND_BARN = 40;

    private PdlKlient pdlKlient;
    private PdlKlientSystem pdlKlientSystem;

    public PdlOppslagTjeneste() {
        // CDI
    }

    @Inject
    public PdlOppslagTjeneste(PdlKlientSystem pdlKlientSystem, PdlKlient pdlKlient) {
        this.pdlKlientSystem = pdlKlientSystem;
        this.pdlKlient = pdlKlient;
    }

    public PersonMedIdent hentSøker(String fnr) {
        var request = new HentPersonQueryRequest();
        request.setIdent(fnr);
        var projection = new PersonResponseProjection()
                .folkeregisteridentifikator(new FolkeregisteridentifikatorResponseProjection().identifikasjonsnummer().status())
                .navn(new NavnResponseProjection().fornavn().mellomnavn().etternavn())
                .kjoenn(new KjoennResponseProjection().kjoenn())
                .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato())
                .doedfoedtBarn(new DoedfoedtBarnResponseProjection().dato())
                .sivilstand(new SivilstandResponseProjection().type())
                .forelderBarnRelasjon(new ForelderBarnRelasjonResponseProjection().relatertPersonsIdent().relatertPersonsRolle().minRolleForPerson());
        var person = pdlKlient.hentPerson(request, projection);
        if (PersonMappers.manglerIdentifikator(person)) {
            var falskId = FalskIdentitet.finnFalskIdentitet(fnr, pdlKlient).orElse(null);
            return new PersonMedIdent(fnr, person, falskId);
        }
        return new PersonMedIdent(fnr, person);
    }

    public List<PersonMedIdent> hentBarnTilSøker(PersonMedIdent søker) {
        var relaterteBarn = relaterteBarn(søker);
        var dødfødteBarn = safeStream(søker.person().getDoedfoedtBarn())
                .filter(d -> d.getDato() != null)
                .map(PdlOppslagTjeneste::dødfødtBarn)
                .map(person -> new PersonMedIdent(null, person))
                .toList();
        return Stream.concat(relaterteBarn.stream(), dødfødteBarn.stream())
                .filter(PdlOppslagTjeneste::barnErYngreEnn40Mnd)
                .toList();
    }

    private List<PersonMedIdent> relaterteBarn(PersonMedIdent søker) {
        var barnIdenter = barnRelatertTil(søker);
        if (barnIdenter.isEmpty()) {
            return List.of();
        }

        var requestBarn = new HentPersonBolkQueryRequest();
        requestBarn.setIdenter(barnIdenter);
        var projeksjonBolk = new HentPersonBolkResultResponseProjection()
                .ident()
                .person(new PersonResponseProjection()
                        .navn(new NavnResponseProjection().fornavn().mellomnavn().etternavn())
                        .kjoenn(new KjoennResponseProjection().kjoenn())
                        .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato())
                        .adressebeskyttelse(new AdressebeskyttelseResponseProjection().gradering())
                        .doedsfall(new DoedsfallResponseProjection().doedsdato())
                        .forelderBarnRelasjon(new ForelderBarnRelasjonResponseProjection().relatertPersonsIdent().relatertPersonsRolle().minRolleForPerson())
                );
        return pdlKlientSystem.hentPersonBolk(requestBarn, projeksjonBolk).stream()
                // Feiler nå hardt hvis person er null (finnes ikke, ugyldig ident)
                // identen kommer fra opprinnelig fra PDL så her antar vi noe er veldig feil hivs dette intreffer
                .filter(b -> !harAdressebeskyttelse(b.getPerson()))
                .map(p -> new PersonMedIdent(p.getIdent(), p.getPerson()))
                .toList();
    }

    private static Person dødfødtBarn(DoedfoedtBarn df) {
        var dødfødtBarn = new Person();
        dødfødtBarn.setFoedselsdato(List.of(new Foedselsdato(df.getDato(), null, null, null)));
        dødfødtBarn.setDoedsfall(List.of(new Doedsfall(df.getDato(), null, null)));
        return dødfødtBarn;
    }

    public Map<String, PersonMedIdent> hentAnnenpartRelatertTilBarn(List<PersonMedIdent> barn, Fødselsnummer søkersFnr) {
        if (barn.isEmpty()) {
            return Map.of();
        }

        var barnTilAnnenpartMapping = barn.stream()
                .filter(barnet -> barnet.ident() != null) // Eksempelvis ved dødfødsel er ikke ident satt
                .map(barnet -> Map.entry(barnet.ident(), annenForelderRegisterertPåBarnet(søkersFnr, barnet)))
                .filter(entry -> entry.getValue().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));

        var annenpartIDenter = barnTilAnnenpartMapping.values().stream().distinct().toList();
        if (annenpartIDenter.isEmpty()) {
            return Map.of();
        }

        var request = new HentPersonBolkQueryRequest();
        request.setIdenter(annenpartIDenter);
        var projeksjonAnnenpart = new HentPersonBolkResultResponseProjection()
                .ident()
                .person(new PersonResponseProjection()
                        .navn(new NavnResponseProjection().fornavn().mellomnavn().etternavn())
                        .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato())
                        .doedsfall(new DoedsfallResponseProjection().doedsdato())
                        .adressebeskyttelse(new AdressebeskyttelseResponseProjection().gradering()));

        var annnepartOppslag = pdlKlientSystem.hentPersonBolk(request, projeksjonAnnenpart).stream()
                // Feiler nå hardt hvis person er null (finnes ikke, ugyldig ident)
                // identen kommer fra opprinnelig fra PDL så her antar vi noe er veldig feil hivs dette intreffer
                .filter(result -> !harAdressebeskyttelse(result.getPerson()))
                .filter(result -> !harDødsdato(result.getPerson()))
                .map(result -> new PersonMedIdent(result.getIdent(), result.getPerson()))
                .collect(Collectors.toMap(PersonMedIdent::ident, person -> person));

        if (annnepartOppslag.isEmpty()) {
            return Map.of();
        }

        return barnTilAnnenpartMapping.entrySet().stream()
                .filter(entry -> annnepartOppslag.containsKey(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> annnepartOppslag.get(entry.getValue())));
    }

    private static boolean harDødsdato(Person person) {
        return person.getDoedsfall() != null && !person.getDoedsfall().isEmpty();
    }

    private static List<String> barnRelatertTil(PersonMedIdent person) {
        return safeStream(person.person().getForelderBarnRelasjon())
                .filter(r -> r.getRelatertPersonsRolle().equals(ForelderBarnRelasjonRolle.BARN))
                .map(ForelderBarnRelasjon::getRelatertPersonsIdent)
                .filter(Objects::nonNull)
                .toList();
    }

    private static Optional<String> annenForelderRegisterertPåBarnet(Fødselsnummer søkersFnr, PersonMedIdent barnet) {
        return safeStream(barnet.person().getForelderBarnRelasjon())
                .filter(r -> !r.getRelatertPersonsRolle().equals(ForelderBarnRelasjonRolle.BARN))
                .map(ForelderBarnRelasjon::getRelatertPersonsIdent)
                .filter(Objects::nonNull)
                .filter(relatertIdent -> !relatertIdent.equals(søkersFnr.value())) // Sjekker at det ikke er søker selv
                .findFirst();
    }

    private static boolean barnErYngreEnn40Mnd(PersonMedIdent barnet) {
        var fødselsdato = Optional.ofNullable(barnet.falskIdentitet()).map(FalskIdentitet.Informasjon::fødselsdato)
            .or(() -> PersonMappers.mapFødselsdato(barnet.person()))
            .orElseThrow();
        return fødselsdato.isAfter(LocalDate.now().minusMonths(IKKE_ELDRE_ENN_40_MND_BARN));
    }

    private static boolean harAdressebeskyttelse(no.nav.pdl.Person barnet) {
        var graderinger = safeStream(barnet.getAdressebeskyttelse())
                .map(Adressebeskyttelse::getGradering)
                .map(PdlKlientSystem::tilGradering)
                .collect(Collectors.toSet());
        return new AdresseBeskyttelse(graderinger).harBeskyttetAdresse();
    }

    // TODO: Skriv om til domenemapping. Denne wrappingen bryter ACL-tanken og eksponerer produsent-internals
    public record PersonMedIdent(String ident, Person person, FalskIdentitet.Informasjon falskIdentitet) {
        public PersonMedIdent(String ident, Person person) {
            this(ident, person, null);
        }
    }
}
