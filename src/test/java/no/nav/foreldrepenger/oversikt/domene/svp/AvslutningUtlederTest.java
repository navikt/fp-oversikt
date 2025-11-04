package no.nav.foreldrepenger.oversikt.domene.svp;

import static java.time.LocalDate.now;
import static no.nav.foreldrepenger.oversikt.domene.svp.AvslutningUtleder.utledDato;
import static no.nav.foreldrepenger.oversikt.domene.svp.AvslutningUtleder.utledÅrsak;
import static no.nav.foreldrepenger.oversikt.domene.svp.ResultatÅrsak.AVSLAG_INNGANGSVILKÅR;
import static no.nav.foreldrepenger.oversikt.domene.svp.ResultatÅrsak.AVSLAG_SØKNADSFRIST;
import static no.nav.foreldrepenger.oversikt.domene.svp.ResultatÅrsak.INNVILGET;
import static no.nav.foreldrepenger.oversikt.domene.svp.ResultatÅrsak.OPPHØR_FØDSEL;
import static no.nav.foreldrepenger.oversikt.domene.svp.ResultatÅrsak.OPPHØR_TIDSPERIODE_FØR_TERMIN;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.kontrakter.fpoversikt.svp.AvslutningÅrsak;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.Prosent;
import no.nav.foreldrepenger.oversikt.domene.svp.ArbeidsforholdUttak.ArbeidsforholdIkkeOppfyltÅrsak;

class AvslutningUtlederTest {

    @Test
    void fødselsdato_hvis_før_termin_minus_3() {
        var fødselsdato = now();
        var avslutningDato = utledDato(new FamilieHendelse(fødselsdato, fødselsdato.plusWeeks(4), 1, null));
        assertThat(avslutningDato).isEqualTo(fødselsdato.minusDays(1));
    }

    @Test
    void termindato_hvis_fødselsdato_ikke_før_termin_minus_3() {
        var fødselsdato = now();
        var termindato = fødselsdato.plusWeeks(2);
        var avslutningDato = utledDato(new FamilieHendelse(fødselsdato, termindato, 1, null));
        assertThat(avslutningDato).isEqualTo(termindato.minusWeeks(3).minusDays(1));
    }

    @Test
    void termindato_minus_3_hvis_ingen_fødselsdato() {
        var termindato = now();
        var avslutningDato = utledDato(new FamilieHendelse(null, termindato, 0, null));
        assertThat(avslutningDato).isEqualTo(termindato.minusWeeks(3).minusDays(1));
    }

    @Test
    void årsak_hentes_fra_ikke_oppylt_på_arbeidsforholdet() {
        var avslutningÅrsak = utledÅrsak(ArbeidsforholdIkkeOppfyltÅrsak.ARBEIDSGIVER_KAN_TILRETTELEGGE_FREM_TIL_3_UKER_FØR_TERMIN, Set.of());
        assertThat(avslutningÅrsak).isEqualTo(AvslutningÅrsak.TILBAKE_I_HEL_STILLING);
    }

    @Test
    void årsak_er_normal_hvis_vedtak_uten_perioder_uten_avslag_på_arbeidsgiver() {
        var avslutningÅrsak = utledÅrsak(null, Set.of());
        assertThat(avslutningÅrsak).isEqualTo(AvslutningÅrsak.NORMAL);
    }

    @Test
    void årsak_er_avslag_inngangsvilår_hvis_alle_perioder_er_avslått_pga_inngangsvilkår() {
        var avslutningÅrsak = utledÅrsak(null,
            Set.of(new SvpPeriode(now(), now(), TilretteleggingType.HEL, Prosent.ZERO, Prosent.ZERO, AVSLAG_INNGANGSVILKÅR)));
        assertThat(avslutningÅrsak).isEqualTo(AvslutningÅrsak.AVSLAG_INNGANGSVILKÅR);
    }

    @Test
    void årsak_hentes_fra_første_periode_med_opphør() {
        var avslåttSøknadsfrist = new SvpPeriode(now(), now(), TilretteleggingType.INGEN, Prosent.ZERO, Prosent.ZERO, AVSLAG_SØKNADSFRIST);
        var innvilget = new SvpPeriode(now().plusDays(1), now().plusDays(1), TilretteleggingType.INGEN, Prosent.ZERO, new Prosent(100), INNVILGET);
        var opphør1 = new SvpPeriode(now().plusDays(2), now().plusDays(2), TilretteleggingType.INGEN, Prosent.ZERO, Prosent.ZERO, OPPHØR_TIDSPERIODE_FØR_TERMIN);
        var opphør2 = new SvpPeriode(now().plusDays(3), now().plusDays(3), TilretteleggingType.INGEN, Prosent.ZERO, Prosent.ZERO, OPPHØR_FØDSEL);
        var avslutningÅrsak = utledÅrsak(null, Set.of(avslåttSøknadsfrist, innvilget, opphør1, opphør2));
        assertThat(avslutningÅrsak).isEqualTo(AvslutningÅrsak.AVSLAG_TIDSPERIODE_FØR_TERMIN);
    }
}
