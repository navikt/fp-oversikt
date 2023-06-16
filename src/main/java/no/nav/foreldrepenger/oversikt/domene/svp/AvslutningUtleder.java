package no.nav.foreldrepenger.oversikt.domene.svp;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Set;

import no.nav.foreldrepenger.common.innsyn.svp.AvslutningÅrsak;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;

final class AvslutningUtleder {

    private AvslutningUtleder() {

    }

    static AvslutningÅrsak utledÅrsak(ArbeidsforholdUttak.ArbeidsforholdIkkeOppfyltÅrsak arbeidsforholdIkkeOppfyltÅrsak,
                                      Set<SvpPeriode> svpPerioder) {
        if (arbeidsforholdIkkeOppfyltÅrsak != null) {
            return switch (arbeidsforholdIkkeOppfyltÅrsak) {
                case ARBEIDSGIVER_KAN_TILRETTELEGGE, ARBEIDSGIVER_KAN_TILRETTELEGGE_FREM_TIL_3_UKER_FØR_TERMIN -> AvslutningÅrsak.TILBAKE_I_HEL_STILLING;
                case ANNET -> AvslutningÅrsak.AVSLAG_ANNET;
            };
        }
        if (svpPerioder.isEmpty()) {
            return AvslutningÅrsak.NORMAL;
        }
        if (svpPerioder.stream().allMatch(p -> p.resultatÅrsak().equals(ResultatÅrsak.AVSLAG_INNGANGSVILKÅR))) {
            return AvslutningÅrsak.AVSLAG_INNGANGSVILKÅR;
        }

        var førsteOpphørsPeriode = svpPerioder.stream()
            .filter(p -> p.resultatÅrsak().erOpphør())
            .min(Comparator.comparing(SvpPeriode::fom));

        return førsteOpphørsPeriode.map(p -> {
            var resultatÅrsak = p.resultatÅrsak();
            return switch (resultatÅrsak) {
                case OPPHØR_OVERGANG_FORELDREPENGER -> AvslutningÅrsak.AVSLAG_OVERGANG_FORELDREPENGER;
                case OPPHØR_FØDSEL -> AvslutningÅrsak.AVSLAG_FØDSEL;
                case OPPHØR_TIDSPERIODE_FØR_TERMIN -> AvslutningÅrsak.AVSLAG_TIDSPERIODE_FØR_TERMIN;
                case OPPHØR_OPPHOLD_I_YTELSEN -> AvslutningÅrsak.TILBAKE_I_HEL_STILLING;
                case OPPHØR_ANNET -> AvslutningÅrsak.AVSLAG_ANNET;
                default -> throw new IllegalStateException("Unexpected value: " + resultatÅrsak);
            };
        }).orElse(AvslutningÅrsak.NORMAL);
    }

    static LocalDate utledDato(FamilieHendelse familieHendelse) {
        var treUkerFørTermin = familieHendelse.termindato().minusWeeks(3);
        var fødselsdato = familieHendelse.fødselsdato();
        var fh = fødselsdato != null && fødselsdato.isBefore(treUkerFørTermin) ? fødselsdato : treUkerFørTermin;
        return fh.minusDays(1);
    }
}
