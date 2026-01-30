package no.nav.foreldrepenger.oversikt.domene.fp;

import java.time.LocalDate;

public record FeriepengeAndel(LocalDate opptjeningsår, Integer årsbeløp, String arbeidsgiverIdent) {}
