package no.nav.foreldrepenger.oversikt.uttaksplan;

import java.util.Objects;

import no.nav.foreldrepenger.oversikt.domene.AktørId;

/**
 * Annen part som kalleren har kontrollert ikke har beskyttet adresse.
 *
 * Konstruktøren er package-private slik at bare {@link UttaksplanRest} kan lage instanser etter
 * adressekontrollen. {@link UttaksplanTjeneste} krever denne typen og kan derfor ikke kalles med
 * en ukontrollert annen part.
 */
public final class VerifisertAnnenPart {

    private final AktørId aktørId;

    VerifisertAnnenPart(AktørId aktørId) {
        this.aktørId = Objects.requireNonNull(aktørId, "aktørId");
    }

    AktørId aktørId() {
        return aktørId;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof VerifisertAnnenPart other && aktørId.equals(other.aktørId);
    }

    @Override
    public int hashCode() {
        return aktørId.hashCode();
    }

    @Override
    public String toString() {
        return "VerifisertAnnenPart[skjermet]";
    }
}
