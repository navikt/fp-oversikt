package no.nav.foreldrepenger.oversikt.server.error;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import no.nav.vedtak.feil.Feilkode;
import no.nav.vedtak.log.util.LoggerUtils;
import no.nav.vedtak.server.rest.FeilUtils;
import no.nav.vedtak.server.rest.ValidationExceptionMapper;

public class LokalValidationExceptionMapper extends ValidationExceptionMapper {

    private static final Logger LOG = LoggerFactory.getLogger(LokalValidationExceptionMapper.class);
    private static final Logger SECURE_LOG = LoggerFactory.getLogger("secureLogger");

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        FeilUtils.ensureCallId();
        var feilmelding = getFeilmeldingTekst(exception);
        LOG.warn(feilmelding);
        // TODO: Er det verdt å beholde denne sikker-loggingen. Ellers kan sentral klasse brukes
        SECURE_LOG.warn("{} - input {}", feilmelding, getInputs(exception));
        return FeilUtils.responseFra(Response.Status.BAD_REQUEST.getStatusCode(), Feilkode.VALIDERING, feilmelding);
    }

    private static Set<String> getInputs(ConstraintViolationException exception) {
        return exception.getConstraintViolations()
            .stream()
            .map(ConstraintViolation::getInvalidValue)
            .filter(Objects::nonNull)
            .map(Object::toString)
            .map(LoggerUtils::removeLineBreaks)
            .collect(Collectors.toSet());
    }

}
