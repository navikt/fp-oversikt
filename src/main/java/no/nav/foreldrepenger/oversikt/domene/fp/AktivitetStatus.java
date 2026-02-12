package no.nav.foreldrepenger.oversikt.domene.fp;

public enum AktivitetStatus {
    ARBEIDSAVKLARINGSPENGER,
    ARBEIDSTAKER,
    DAGPENGER,
    FRILANSER,
    MILITÆR_ELLER_SIVIL,
    SELVSTENDIG_NÆRINGSDRIVENDE,
    KOMBINERT_AT_FL,
    KOMBINERT_AT_SN,
    KOMBINERT_FL_SN,
    KOMBINERT_AT_FL_SN,
    BRUKERS_ANDEL,
    KUN_YTELSE;

    public no.nav.foreldrepenger.kontrakter.felles.kodeverk.AktivitetStatus tilDto() {
        return switch (this) {
            case ARBEIDSAVKLARINGSPENGER -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.AktivitetStatus.ARBEIDSAVKLARINGSPENGER;
            case ARBEIDSTAKER -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.AktivitetStatus.ARBEIDSTAKER;
            case DAGPENGER -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.AktivitetStatus.DAGPENGER;
            case FRILANSER -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.AktivitetStatus.FRILANSER;
            case MILITÆR_ELLER_SIVIL -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.AktivitetStatus.MILITÆR_ELLER_SIVIL;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE;
            case KOMBINERT_AT_FL -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.AktivitetStatus.KOMBINERT_AT_FL;
            case KOMBINERT_AT_SN -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.AktivitetStatus.KOMBINERT_AT_SN;
            case KOMBINERT_FL_SN -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.AktivitetStatus.KOMBINERT_FL_SN;
            case KOMBINERT_AT_FL_SN -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.AktivitetStatus.KOMBINERT_AT_FL_SN;
            case BRUKERS_ANDEL -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.AktivitetStatus.BRUKERS_ANDEL;
            case KUN_YTELSE -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.AktivitetStatus.KUN_YTELSE;
        };
    }
}
