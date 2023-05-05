package no.nav.foreldrepenger.oversikt.domene;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = SakFP0.class, name = "FP0"),
    @JsonSubTypes.Type(value = SakSVP0.class, name = "SVP0"),
    @JsonSubTypes.Type(value = SakES0.class, name = "ES0")
})
public interface Sak {

    no.nav.foreldrepenger.common.innsyn.Sak tilSakDto();

    Saksnummer saksnummer();

    AktørId aktørId();
}
