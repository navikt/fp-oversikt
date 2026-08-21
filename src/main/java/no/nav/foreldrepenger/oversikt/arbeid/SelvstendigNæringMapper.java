package no.nav.foreldrepenger.oversikt.arbeid;

import no.nav.foreldrepenger.kontrakter.fpoversikt.SelvstendigNæring;
import no.nav.foreldrepenger.oversikt.integrasjoner.brreg.BrregSelvstendigNæring;

final class SelvstendigNæringMapper {

    private SelvstendigNæringMapper() {
    }

    static SelvstendigNæring fra(BrregSelvstendigNæring næring) {
        return new SelvstendigNæring(
            næring.organisasjonsnummer(),
            næring.navn(),
            mapVirksomhetstype(næring.næringstype())
        );
    }

    private static SelvstendigNæring.Virksomhetstype mapVirksomhetstype(
        no.nav.foreldrepenger.oversikt.integrasjoner.brreg.Virksomhetstype virksomhetstype) {
        return switch (virksomhetstype) {
            case ANNEN -> SelvstendigNæring.Virksomhetstype.ANNEN;
            case JORDBRUK_SKOGBRUK -> SelvstendigNæring.Virksomhetstype.JORDBRUK_SKOGBRUK;
            case FISKE -> SelvstendigNæring.Virksomhetstype.FISKE;
            case DAGMAMMA -> SelvstendigNæring.Virksomhetstype.DAGMAMMA;
        };
    }
}
