package no.nav.foreldrepenger.oversikt.arbeid;

import jakarta.validation.constraints.NotNull;
import no.nav.foreldrepenger.oversikt.integrasjoner.brreg.BrregSelvstendigNæring;

public record SelvstendigNæringDto(@NotNull String organisasjonsnummer,
                                   String navn,
                                   @NotNull Virksomhetstype næringstype) {

    static SelvstendigNæringDto fra(BrregSelvstendigNæring næring) {
        return new SelvstendigNæringDto(
            næring.organisasjonsnummer(),
            næring.navn(),
            mapVirksomhetstype(næring.næringstype())
        );
    }

    private static Virksomhetstype mapVirksomhetstype(
        no.nav.foreldrepenger.oversikt.integrasjoner.brreg.Virksomhetstype virksomhetstype) {
        return switch (virksomhetstype) {
            case ANNEN -> Virksomhetstype.ANNEN;
            case JORDBRUK_SKOGBRUK -> Virksomhetstype.JORDBRUK_SKOGBRUK;
            case FISKE -> Virksomhetstype.FISKE;
            case DAGMAMMA -> Virksomhetstype.DAGMAMMA;
        };
    }
}
