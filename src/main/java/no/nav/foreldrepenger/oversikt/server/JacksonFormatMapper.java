package no.nav.foreldrepenger.oversikt.server;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.format.jackson.Jackson3JsonFormatMapper;

import no.nav.vedtak.mapper.json.DefaultJsonMapper;

public class JacksonFormatMapper implements FormatMapper {

    private static final FormatMapper FORMAT_MAPPER = new Jackson3JsonFormatMapper(DefaultJsonMapper.getJsonMapper());

    public JacksonFormatMapper() {
        // CDI
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
