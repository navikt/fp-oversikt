package no.nav.foreldrepenger.oversikt.domene;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import no.nav.foreldrepenger.oversikt.domene.es.SakES0;
import no.nav.foreldrepenger.oversikt.domene.fp.SakFP0;
import no.nav.foreldrepenger.oversikt.domene.svp.SakSVP0;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontroll;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = SakFP0.class, name = "FP0"),
    @JsonSubTypes.Type(value = SakSVP0.class, name = "SVP0"),
    @JsonSubTypes.Type(value = SakES0.class, name = "ES0")
})
public interface Sak {

    no.nav.foreldrepenger.common.innsyn.Sak tilSakDto(TilgangKontroll tilgangKontroll);

    Saksnummer saksnummer();

    AktørId aktørId();

    boolean harSøknad();

    LocalDateTime oppdatertTidspunkt();

    boolean harVedtak();

    boolean avsluttet();

    boolean erKomplettForVisning();

    YtelseType ytelse();

    default boolean erHenlagt() {
        return !harVedtak() && avsluttet();
    }
}
