package no.nav.foreldrepenger.oversikt.server;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.saker.UmyndigBrukerException;

class GeneralRestExceptionMapperTest {

    private static final GeneralRestExceptionMapper mapper = new GeneralRestExceptionMapper();

    @Test
    void umyndigBrukerException_gir_403_med_problem_details() {
        var throwable = new UmyndigBrukerException();
        var response = mapper.toResponse(throwable);
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getEntity())
            .isInstanceOf(ProblemDetails.class)
            .extracting("error", "status")
            .contains("UMYNDIG_BRUKER", 403);
    }

    @Test
    void annen_exception_gir_500_uten_problem_details() {
        var throwable = new IllegalStateException("Test");
        var response = mapper.toResponse(throwable);
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getEntity()).isNull();
    }


}
