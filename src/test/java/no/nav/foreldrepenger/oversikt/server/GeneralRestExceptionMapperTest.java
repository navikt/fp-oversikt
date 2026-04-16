package no.nav.foreldrepenger.oversikt.server;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.tilgangskontroll.LokalFeilKode;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.OversiktManglerTilgangException;
import no.nav.vedtak.feil.FeilDto;
import no.nav.vedtak.feil.Feilkode;
import no.nav.vedtak.server.rest.GeneralRestExceptionMapper;


class GeneralRestExceptionMapperTest {

    private static final GeneralRestExceptionMapper mapper = new GeneralRestExceptionMapper();

    @Test
    void umyndigBrukerException_gir_403_med_problem_details() {
        var throwable = new OversiktManglerTilgangException(LokalFeilKode.IKKE_TILGANG_UMYNDIG);
        try (var response = mapper.toResponse(throwable)) {
            assertThat(response.getStatus()).isEqualTo(403);
            assertThat(response.getEntity()).isInstanceOf(FeilDto.class)
                .extracting(e -> ((FeilDto) e).feilkode())
                .isEqualTo(LokalFeilKode.IKKE_TILGANG_UMYNDIG.name());
        }
    }

    @Test
    void annen_exception_gir_500_generell() {
        var throwable = new IllegalStateException("Test");
        try (var response = mapper.toResponse(throwable)) {
            assertThat(response.getStatus()).isEqualTo(500);
            assertThat(response.getEntity()).isInstanceOf(FeilDto.class)
                .extracting(e -> ((FeilDto) e).feilkode())
                .isEqualTo(Feilkode.GENERELL.name());
        }
    }


}
