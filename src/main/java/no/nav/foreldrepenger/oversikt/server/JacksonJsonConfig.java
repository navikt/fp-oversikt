package no.nav.foreldrepenger.oversikt.server;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@Provider
public class JacksonJsonConfig implements ContextResolver<ObjectMapper> {

    private static final ObjectMapper MAPPER = DefaultJsonMapper.getObjectMapper();

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return MAPPER;
    }
}
