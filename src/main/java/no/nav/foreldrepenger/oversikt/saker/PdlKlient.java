package no.nav.foreldrepenger.oversikt.saker;

import java.time.LocalDate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.AdresseBeskyttelse;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.FødseldatoOppslag;
import no.nav.pdl.Adressebeskyttelse;
import no.nav.pdl.AdressebeskyttelseGradering;
import no.nav.pdl.AdressebeskyttelseResponseProjection;
import no.nav.pdl.Foedsel;
import no.nav.pdl.FoedselResponseProjection;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.PersonResponseProjection;
import no.nav.vedtak.felles.integrasjon.person.AbstractPersonKlient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@RestClientConfig(
    tokenConfig = TokenFlow.ADAPTIVE,
    endpointProperty = "pdl.base.url",
    endpointDefault = "https://pdl-api.prod-fss-pub.nais.io/graphql",
    scopesProperty = "pdl.scopes",
    scopesDefault = "api://prod-fss.pdl.pdl-api/.default")
@Dependent
public class PdlKlient extends AbstractPersonKlient implements FødselsnummerOppslag, FødseldatoOppslag, AktørIdOppslag, AdresseBeskyttelseOppslag {

    private static final Logger LOG = LoggerFactory.getLogger(PdlKlient.class);

    @Override
    public Fødselsnummer forAktørId(AktørId aktørId) {
        LOG.debug("Mapper aktørId til fnr");
        return new Fødselsnummer(hentPersonIdentForAktørId(aktørId.value()).orElseThrow());
    }

    @Override
    public AktørId forFnr(Fødselsnummer fnr) {
        LOG.debug("Mapper fnr til aktørId");
        var a = hentAktørIdForPersonIdent(fnr.value()).orElseThrow();
        return new AktørId(a);
    }

    @Override
    public LocalDate fødselsdato(String fnr) {
        LOG.debug("Henter fødselsdato");
        var request = new HentPersonQueryRequest();
        request.setIdent(fnr);
        var projection = new PersonResponseProjection()
            .foedsel(new FoedselResponseProjection().foedselsdato());
        var person = hentPerson(request, projection);
        return person.getFoedsel().stream()
            .findFirst()
            .map(Foedsel::getFoedselsdato)
            .map(LocalDate::parse)
            .orElseThrow();
    }

    @Override
    public AdresseBeskyttelse adresseBeskyttelse(Fødselsnummer fnr) {
        var request = new HentPersonQueryRequest();
        request.setIdent(fnr.value());
        var projection = new PersonResponseProjection()
            .adressebeskyttelse(new AdressebeskyttelseResponseProjection().gradering());
        var person = hentPerson(request, projection, true);

        if (person == null) {
            throw new BrukerIkkeFunnetIPdlException();
        }

        var gradering = person.getAdressebeskyttelse().stream()
            .map(Adressebeskyttelse::getGradering)
            .map(PdlKlient::tilGradering)
            .collect(Collectors.toSet());
        return new AdresseBeskyttelse(gradering);
    }

    private static AdresseBeskyttelse.Gradering tilGradering(AdressebeskyttelseGradering adressebeskyttelseGradering) {
        if (adressebeskyttelseGradering == null) {
            return AdresseBeskyttelse.Gradering.UGRADERT;
        }
        return switch (adressebeskyttelseGradering) {
            case STRENGT_FORTROLIG_UTLAND, STRENGT_FORTROLIG, FORTROLIG -> AdresseBeskyttelse.Gradering.GRADERT;
            case UGRADERT -> AdresseBeskyttelse.Gradering.UGRADERT;
        };
    }

}
