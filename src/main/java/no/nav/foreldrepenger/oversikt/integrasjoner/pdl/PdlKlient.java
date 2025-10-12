package no.nav.foreldrepenger.oversikt.integrasjoner.pdl;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.pdl.FoedselsdatoResponseProjection;
import no.nav.pdl.FolkeregisteridentifikatorResponseProjection;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.PersonResponseProjection;
import no.nav.vedtak.felles.integrasjon.person.AbstractPersonKlient;
import no.nav.vedtak.felles.integrasjon.person.FalskIdentitet;
import no.nav.vedtak.felles.integrasjon.person.PersonMappers;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.util.LRUCache;

@RestClientConfig(
    tokenConfig = TokenFlow.ADAPTIVE,
    endpointProperty = "pdl.base.url",
    endpointDefault = "https://pdl-api.prod-fss-pub.nais.io/graphql",
    scopesProperty = "pdl.scopes",
    scopesDefault = "api://prod-fss.pdl.pdl-api/.default")
@Dependent
public class PdlKlient extends AbstractPersonKlient {

    private static final Logger LOG = LoggerFactory.getLogger(PdlKlient.class);

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(60, TimeUnit.MINUTES);
    private static final LRUCache<String, LocalDate> FNR_FØDT = new LRUCache<>(1000, CACHE_ELEMENT_LIVE_TIME_MS);
    private static final LRUCache<String, AktørId> FNR_AKTØR = new LRUCache<>(2000, CACHE_ELEMENT_LIVE_TIME_MS);

    public LocalDate fødselsdato(String fnr) {
        LOG.debug("Henter fødselsdato");
        if (FNR_FØDT.get(fnr) != null) {
            return FNR_FØDT.get(fnr);
        }
        var request = new HentPersonQueryRequest();
        request.setIdent(fnr);
        var projection = new PersonResponseProjection()
            .folkeregisteridentifikator(new FolkeregisteridentifikatorResponseProjection().identifikasjonsnummer().status())
            .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato());
        var person = hentPerson(request, projection);
        if (PersonMappers.manglerIdentifikator(person)) {
            var falskId = FalskIdentitet.finnFalskIdentitet(fnr, this).orElse(null);
            if (falskId != null) {
                FNR_FØDT.put(fnr, falskId.fødselsdato());
                return falskId.fødselsdato();
            }
        }
        var fødselsdato = PersonMappers.mapFødselsdato(person).orElseThrow();
        FNR_FØDT.put(fnr, fødselsdato);
        return fødselsdato;
    }

    public AktørId aktørId(String fnr) {
        return Optional.ofNullable(FNR_AKTØR.get(fnr))
                .orElseGet(() -> {
                    var aktørId = new AktørId(hentAktørIdForPersonIdent(fnr).orElseThrow());
                    FNR_AKTØR.put(fnr, aktørId);
                    return aktørId;
                });
    }
}
