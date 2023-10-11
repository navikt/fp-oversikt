package no.nav.foreldrepenger.oversikt.saker;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
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
class PdlKlient extends AbstractPersonKlient {

    private static final Logger LOG = LoggerFactory.getLogger(PdlKlient.class);

    LocalDate fødselsdato(String fnr) {
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
}
