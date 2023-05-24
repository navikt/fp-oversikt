package no.nav.foreldrepenger.oversikt.domene;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import no.nav.foreldrepenger.oversikt.saker.FødselsnummerOppslag;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = SakFP0.class, name = "FP0"),
    @JsonSubTypes.Type(value = SakSVP0.class, name = "SVP0"),
    @JsonSubTypes.Type(value = SakES0.class, name = "ES0")
})
public interface Sak {

    no.nav.foreldrepenger.common.innsyn.Sak tilSakDto(FødselsnummerOppslag fødselsnummerOppslag);

    Saksnummer saksnummer();

    AktørId aktørId();

    boolean harSakSøknad();

    LocalDateTime oppdatertTidspunkt();
}
