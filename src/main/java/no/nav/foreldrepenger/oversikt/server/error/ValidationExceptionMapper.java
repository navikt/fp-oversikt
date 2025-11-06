package no.nav.foreldrepenger.oversikt.server.error;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {
    private static final Logger LOG = LoggerFactory.getLogger(ValidationExceptionMapper.class);
    private static final Logger SECURE_LOG = LoggerFactory.getLogger("secureLogger");


    @Override
    public Response toResponse(ConstraintViolationException exception) {
        LOG.warn("Det oppstod en valideringsfeil: {}", constraints(exception));
        SECURE_LOG.warn("Det oppstod en valideringsfeil: felt {} - input {}", constraints(exception), getInputs(exception));
        return Response.status(Response.Status.BAD_REQUEST)
                       .entity("Validation failed: " + exception.getMessage())
                       .build();
    }

    private static Set<String> getInputs(ConstraintViolationException exception) {
        return exception.getConstraintViolations()
            .stream()
            .map(ConstraintViolation::getInvalidValue)
            .filter(Objects::nonNull)
            .map(Object::toString)
            .collect(Collectors.toSet());
    }

    private static Set<String> constraints(ConstraintViolationException exception) {
        return exception.getConstraintViolations()
            .stream()
            .map(cv -> cv.getRootBeanClass().getSimpleName() + "." + cv.getLeafBean().getClass().getSimpleName() + "." + fieldName(cv) + " - "
                + cv.getMessage())
            .collect(Collectors.toSet());
    }

    private static String fieldName(ConstraintViolation<?> cv) {
        String field = null;
        for (var node : cv.getPropertyPath()) {
            field = node.getName();
        }
        return field;
    }

}
