package no.nav.foreldrepenger.oversikt.arbeid;

import no.nav.foreldrepenger.oversikt.integrasjoner.aareg.ArbeidType;

public record ArbeidsforholdIdentifikator(String arbeidsgiver,
                                          String arbeidsforholdId,
                                          ArbeidType arbeidType) {
}
