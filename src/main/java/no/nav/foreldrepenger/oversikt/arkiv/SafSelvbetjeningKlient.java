package no.nav.foreldrepenger.oversikt.arkiv;


import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLOperationRequest;
import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLRequest;
import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLResponseProjection;
import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLResult;
import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.core.UriBuilder;
import no.nav.safselvbetjening.Dokumentoversikt;
import no.nav.safselvbetjening.DokumentoversiktResponseProjection;
import no.nav.safselvbetjening.DokumentoversiktSelvbetjeningQueryRequest;
import no.nav.safselvbetjening.DokumentoversiktSelvbetjeningQueryResponse;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.felles.integrasjon.saf.SafErrorHandler;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@RestClientConfig(
    tokenConfig = TokenFlow.ADAPTIVE,
    endpointProperty = "safselvbetjening.base.url",
    endpointDefault = "https://safselvbetjening.prod-fss-pub.nais.io",
    scopesProperty = "safselvbetjening.scopes",
    scopesDefault = "api://prod-fss.teamdokumenthandtering.safselvbetjening/.default")
@Dependent
public class SafSelvbetjeningKlient implements SafSelvbetjening {
    private static final String F_240613 = "F-240613";
    private static final String HENT_DOKUMENT_PATH = "/rest/hentdokument/{journalpostId}/{dokumentInfoId}/ARKIV";
    private static final String GRAPHQL_PATH = "/graphql";
    private final RestClient restKlient;
    private final RestConfig restConfig;
    private final URI graphql;
    private final SafErrorHandler errorHandler;

    public SafSelvbetjeningKlient() {
        this.restKlient = RestClient.client();
        this.restConfig = RestConfig.forClient(this.getClass());
        this.graphql = URI.create(this.restConfig.endpoint() + GRAPHQL_PATH);
        this.errorHandler = new SafErrorHandler();
    }

    @Override
    public HttpResponse<byte[]> dokument(JournalpostId journalpostId, DokumentId dokumentId) {
        var path = UriBuilder.fromUri(restConfig.endpoint())
            .path(HENT_DOKUMENT_PATH)
            .resolveTemplate("journalpostId", journalpostId.verdi())
            .resolveTemplate("dokumentInfoId", dokumentId.verdi())
            .build();
        var request = RestRequest.newGET(path, restConfig);
        return restKlient.sendReturnResponseByteArray(request);
    }

    @Override
    public Dokumentoversikt dokumentoversiktSelvbetjening(DokumentoversiktSelvbetjeningQueryRequest q, DokumentoversiktResponseProjection p) {
        return query(q, p, DokumentoversiktSelvbetjeningQueryResponse.class).dokumentoversiktSelvbetjening();
    }

    private <T extends GraphQLResult<?>> T query(GraphQLOperationRequest req, GraphQLResponseProjection p, Class<T> clazz) {
        return query(new GraphQLRequest(req, p), clazz);
    }

    private <T extends GraphQLResult<?>> T query(GraphQLRequest req, Class<T> clazz) {
        var method = new RestRequest.Method(RestRequest.WebMethod.POST, HttpRequest.BodyPublishers.ofString(req.toHttpJsonBody()));
        var request = RestRequest.newRequest(method, graphql, restConfig);
        var res = restKlient.send(request, clazz);
        if (res.hasErrors()) {
            return errorHandler.handleError(res.getErrors(), restConfig.endpoint(), F_240613);
        }
        return res;
    }
}
