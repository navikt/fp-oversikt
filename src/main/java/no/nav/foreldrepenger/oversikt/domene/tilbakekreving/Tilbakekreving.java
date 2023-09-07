package no.nav.foreldrepenger.oversikt.domene.tilbakekreving;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TilbakekrevingV1.class, name = "1")
})
public interface Tilbakekreving {
    Saksnummer saksnummer();
}
