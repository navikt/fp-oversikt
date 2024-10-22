package no.nav.foreldrepenger.oversikt.saker;

import jakarta.enterprise.context.Dependent;
import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.vedtak.felles.integrasjon.person.AbstractPersonKlient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@RestClientConfig(
    tokenConfig = TokenFlow.AZUREAD_CC,
    endpointProperty = "pdl.base.url",
    endpointDefault = "https://pdl-api.prod-fss-pub.nais.io/graphql",
    scopesProperty = "pdl.scopes",
    scopesDefault = "api://prod-fss.pdl.pdl-api/.default")
@Dependent
class PdlKlientSystem extends AbstractPersonKlient implements PersonOppslagSystem {

    @Override
    public Fødselsnummer fødselsnummer(AktørId aktørId) {
        return new Fødselsnummer(hentPersonIdentForAktørId(aktørId.value()).orElseThrow());
    }

    @Override
    public AktørId aktørId(Fødselsnummer fnr) {
        return new AktørId(hentAktørIdForPersonIdent(fnr.value()).orElseThrow());
    }

}
