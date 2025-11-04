package no.nav.foreldrepenger.oversikt.domene;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record Arbeidsgiver(@JsonValue String identifikator) {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public Arbeidsgiver {
        //jackson
    }

    public static Arbeidsgiver dummy() {
        return new Arbeidsgiver(UUID.randomUUID().toString().substring(0, 9));
    }

    @Override
    public String toString() {
        return "Arbeidsgiver{" + "identifikator='***'}'";
    }

    public no.nav.foreldrepenger.kontrakter.fpoversikt.Arbeidsgiver tilDto() {
        var arbeidsgiverType = erOrg() ? no.nav.foreldrepenger.kontrakter.fpoversikt.Arbeidsgiver.ArbeidsgiverType.ORGANISASJON :
            no.nav.foreldrepenger.kontrakter.fpoversikt.Arbeidsgiver.ArbeidsgiverType.PRIVAT;
        return new no.nav.foreldrepenger.kontrakter.fpoversikt.Arbeidsgiver(identifikator, arbeidsgiverType);
    }

    private boolean erOrg() {
        return identifikator.length() == 9;
    }
}
