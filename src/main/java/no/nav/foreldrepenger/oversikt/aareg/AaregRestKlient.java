package no.nav.foreldrepenger.oversikt.aareg;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriBuilderException;
import no.nav.vedtak.felles.integrasjon.rest.NavHeaders;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;


/*
 * Dokumentasjon https://confluence.adeo.no/display/FEL/AAREG+-+Tjeneste+REST+aareg.api
 * Swagger https://aareg-services-q2.dev.intern.nav.no/swagger-ui/index.html?urls.primaryName=aareg.api.v1#/arbeidstaker/finnArbeidsforholdPrArbeidstaker
 * Swagger V2 https://aareg-services-q2.dev.intern.nav.no/swagger-ui/index.html?urls.primaryName=aareg.api.v2#/arbeidstaker/finnArbeidsforholdPrArbeidstaker
 */
@Dependent
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, endpointProperty = "aareg.rs.url",
    endpointDefault = "https://aareg-services.prod-fss-pub.nais.io/api/v1/arbeidstaker",
    scopesProperty = "aareg.scopes", scopesDefault = "api://prod-fss.arbeidsforhold.aareg-services-nais/.default")
public class AaregRestKlient {

    private final RestClient sender;
    private final RestConfig restConfig;

    public AaregRestKlient() {
        this.sender = RestClient.client();
        this.restConfig = RestConfig.forClient(this.getClass());
    }

    public List<ArbeidsforholdRS> finnArbeidsforholdForArbeidstaker(String ident, LocalDate qfom, LocalDate qtom, boolean historikk) {
        try {
            var query = UriBuilder.fromUri(restConfig.endpoint()).path("arbeidsforhold")
                .queryParam("ansettelsesperiodeFom", String.valueOf(qfom))
                .queryParam("ansettelsesperiodeTom", String.valueOf(qtom))
                .queryParam("regelverk", "A_ORDNINGEN")
                .queryParam("historikk", String.valueOf(historikk))
                .queryParam("sporingsinformasjon", "false")
                .build();
            var request = RestRequest.newGET(query, restConfig)
                .header(NavHeaders.HEADER_NAV_PERSONIDENT, ident);
            var result = sender.send(request, ArbeidsforholdRS[].class);
            return Arrays.asList(result);
        } catch (IllegalArgumentException|UriBuilderException e) {
            throw new IllegalArgumentException("Utviklerfeil syntax-exception for finnArbeidsforholdForArbeidstaker");
        }
    }

}
