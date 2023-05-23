package no.nav.foreldrepenger.oversikt.saker;

import javax.enterprise.context.Dependent;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.FødseldatoOppslag;
import no.nav.vedtak.felles.integrasjon.person.AbstractPersonKlient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

@RestClientConfig(
    tokenConfig = TokenFlow.ADAPTIVE,
    endpointProperty = "pdl.base.url",
    endpointDefault = "https://pdl-api.prod-fss-pub.nais.io/graphql",
    scopesProperty = "pdl.scopes",
    scopesDefault = "api://prod-fss.pdl.pdl-api/.default")
@Dependent
public class PdlKlient extends AbstractPersonKlient implements FødselsnummerOppslag, FødseldatoOppslag {

    private static final Logger LOG = LoggerFactory.getLogger(PdlKlient.class);

    @Override
    public String forAktørId(AktørId aktørId) {
        LOG.info("Mapper aktørId til fnr");
        return hentPersonIdentForAktørId(aktørId.value()).orElseThrow();
    }

    @Override
    public LocalDate fødselsdato(String fnr) {
        return null;
    }
}
