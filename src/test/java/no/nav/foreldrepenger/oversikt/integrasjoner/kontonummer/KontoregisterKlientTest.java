package no.nav.foreldrepenger.oversikt.integrasjoner.kontonummer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import javax.net.ssl.SSLSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.vedtak.felles.integrasjon.rest.RestClient;

@ExtendWith(MockitoExtension.class)
class KontoregisterKlientTest {

    @Mock
    private RestClient restClient;
    private KontoregisterKlient kontoregisterKlient;

    @BeforeEach
    void setUp() {
        kontoregisterKlient = new KontoregisterKlient(restClient);
    }

    @Test
    void norsk_kontonummer_oppslag_test() {
        var body = """
                {
                    "kontonummer": "123"
                }
                """;
        when(restClient.sendReturnUnhandled(any())).thenReturn(new HttpResponseImpl(200, body));
        assertThat(kontoregisterKlient.hentRegistrertKontonummer().orElseThrow()).isEqualTo("123");
    }

    @Test
    void kontonummer_feiler_med_404_skal_returnere_ukjent_kontonummer_og_ikke_feile() {
        when(restClient.sendReturnUnhandled(any())).thenReturn(new HttpResponseImpl(404, null));
        assertThat(kontoregisterKlient.hentRegistrertKontonummer()).isEmpty();
    }

    @Test
    void andre_status_kode_skal_hive_exception_men_catches_og_returner_ukjent_men_logges() {
        when(restClient.sendReturnUnhandled(any())).thenReturn(new HttpResponseImpl(500, null));
        assertThat(kontoregisterKlient.hentRegistrertKontonummer()).isEmpty();
    }

    record HttpResponseImpl(int status, String body) implements HttpResponse<String> {
        @Override
        public int statusCode() {
            return status;
        }

        @Override
        public HttpRequest request() {
            return null;
        }

        @Override
        public Optional<HttpResponse<String>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return null;
        }

        @Override
        public String body() {
            return body;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return null;
        }

        @Override
        public HttpClient.Version version() {
            return null;
        }
    }
}
