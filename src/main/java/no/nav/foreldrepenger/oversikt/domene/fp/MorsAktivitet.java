package no.nav.foreldrepenger.oversikt.domene.fp;

public enum MorsAktivitet {
    ARBEID,
    UTDANNING,
    KVALPROG,
    INTROPROG,
    TRENGER_HJELP,
    INNLAGT,
    ARBEID_OG_UTDANNING,
    UFØRE,
    IKKE_OPPGITT;

    public no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet tilDto() {
        return switch (this) {
            case ARBEID -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet.ARBEID;
            case UTDANNING -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet.UTDANNING;
            case KVALPROG -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet.KVALPROG;
            case INTROPROG -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet.INTROPROG;
            case TRENGER_HJELP -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet.TRENGER_HJELP;
            case INNLAGT -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet.INNLAGT;
            case ARBEID_OG_UTDANNING -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet.ARBEID_OG_UTDANNING;
            case UFØRE -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet.UFØRE;
            case IKKE_OPPGITT -> no.nav.foreldrepenger.kontrakter.felles.kodeverk.MorsAktivitet.IKKE_OPPGITT;
        };
    }
}
