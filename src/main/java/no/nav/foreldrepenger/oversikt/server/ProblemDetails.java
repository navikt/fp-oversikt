package no.nav.foreldrepenger.oversikt.server;

import no.nav.foreldrepenger.oversikt.tilgangskontroll.FeilKode;

public record ProblemDetails(FeilKode feilKode, int status, String message) {

}
