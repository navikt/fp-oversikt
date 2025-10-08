package no.nav.foreldrepenger.oversikt.server;

import java.util.TimeZone;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

@Provider
public class JacksonJsonConfig implements ContextResolver<ObjectMapper>, FormatMapper {


    private static final ObjectMapper MAPPER = new ObjectMapper() // TODO: Bytt til DefaultJsonMapper når .setSerializationInclusion(JsonInclude.Include.NON_EMPTY) fjernes i felles
        .registerModule(new Jdk8Module())
        .registerModule(new JavaTimeModule())
        .setTimeZone(TimeZone.getTimeZone("Europe/Oslo"))
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
        //.setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
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
