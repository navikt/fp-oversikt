package no.nav.foreldrepenger.oversikt.server;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import no.nav.foreldrepenger.oversikt.tilgangskontroll.FeilKode;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.ManglerTilgangException;

import org.junit.jupiter.api.Test;


class GeneralRestExceptionMapperTest {

    private static final GeneralRestExceptionMapper mapper = new GeneralRestExceptionMapper();

    @Test
    void umyndigBrukerException_gir_403_med_problem_details() {
        var throwable = new ManglerTilgangException(FeilKode.IKKE_TILGANG_UMYNDIG);
        var response = mapper.toResponse(throwable);
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getEntity())
            .isInstanceOf(ProblemDetails.class)
            .extracting("feilKode", "status")
            .contains(FeilKode.IKKE_TILGANG_UMYNDIG, 403);
    }

    @Test
    void annen_exception_gir_500_uten_problem_details() {
        var throwable = new IllegalStateException("Test");
        var response = mapper.toResponse(throwable);
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getEntity()).isNull();
    }


}
