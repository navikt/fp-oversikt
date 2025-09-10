package no.nav.foreldrepenger.oversikt.server;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@Provider
public class JacksonJsonConfig implements ContextResolver<ObjectMapper>, FormatMapper {


    private static final ObjectMapper MAPPER = DefaultJsonMapper.getObjectMapper();
    private static final FormatMapper FORMAT_MAPPER = new JacksonJsonFormatMapper(MAPPER);

    public JacksonJsonConfig() {
        // CDI
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return MAPPER;
    }

    @Override
    public <T> T fromString(CharSequence charSequence, JavaType<T> javaType, WrapperOptions wrapperOptions) {
        return FORMAT_MAPPER.fromString(charSequence, javaType, wrapperOptions);
    }

    @Override
    public <T> String toString(T t, JavaType<T> javaType, WrapperOptions wrapperOptions) {
        return FORMAT_MAPPER.toString(t, javaType, wrapperOptions);
    }
}
