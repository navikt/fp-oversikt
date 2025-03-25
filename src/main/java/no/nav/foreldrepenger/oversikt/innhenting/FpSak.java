package no.nav.foreldrepenger.oversikt.innhenting;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;
import no.nav.foreldrepenger.oversikt.domene.Prosent;
import no.nav.foreldrepenger.oversikt.domene.fp.Trekkdager;


public record FpSak(String saksnummer,
                    String aktørId,
                    FamilieHendelse familieHendelse,
                    boolean avsluttet,
                    Set<Vedtak> vedtak,
                    String oppgittAnnenPart,
                    Set<Aksjonspunkt> aksjonspunkt,
                    Set<Søknad> søknader,
                    BrukerRolle brukerRolle,
                    Set<String> fødteBarn,
                    Rettigheter rettigheter,
                    boolean ønskerJustertUttakVedFødsel) implements Sak {

    public enum Dekningsgrad {
        ÅTTI,
        HUNDRE
    }

    public record Vedtak(LocalDateTime vedtakstidspunkt, List<Uttaksperiode> uttaksperioder, FpSak.Dekningsgrad dekningsgrad) {
    }

    public record Uttaksperiode(LocalDate fom, LocalDate tom, UtsettelseÅrsak utsettelseÅrsak, OppholdÅrsak oppholdÅrsak,
                                OverføringÅrsak overføringÅrsak, Prosent samtidigUttak, Boolean flerbarnsdager,
                                MorsAktivitet morsAktivitet, Resultat resultat) {

        public record Resultat(Type type, Årsak årsak, Set<UttaksperiodeAktivitet> aktiviteter, boolean trekkerMinsterett) {

            public enum Type {
                INNVILGET,
                INNVILGET_GRADERING,
                AVSLÅTT
            }

            public enum Årsak {
                ANNET,
                AVSLAG_HULL_I_UTTAKSPLAN,
                AVSLAG_UTSETTELSE_TILBAKE_I_TID,
                INNVILGET_UTTAK_AVSLÅTT_GRADERING_TILBAKE_I_TID,
                AVSLAG_FRATREKK_PLEIEPENGER
            }
        }

        public record UttaksperiodeAktivitet(UttakAktivitet aktivitet, Konto konto, Trekkdager trekkdager, Prosent arbeidstidsprosent) {

        }

    }
    public record UttakAktivitet(UttakAktivitet.Type type, Arbeidsgiver arbeidsgiver, String arbeidsforholdId) {
        public enum Type {
            ORDINÆRT_ARBEID,
            SELVSTENDIG_NÆRINGSDRIVENDE,
            FRILANS,
            ANNET
        }
    }
    public record Søknad(SøknadStatus status, LocalDateTime mottattTidspunkt, Set<Periode> perioder, Dekningsgrad dekningsgrad,
                         boolean morArbeidUtenDok) {

        public record Periode(LocalDate fom, LocalDate tom, Konto konto, UtsettelseÅrsak utsettelseÅrsak, OppholdÅrsak oppholdÅrsak,
                              OverføringÅrsak overføringÅrsak, Gradering gradering, Prosent samtidigUttak, Boolean flerbarnsdager,
                              MorsAktivitet morsAktivitet) {

        }
    }

    public enum BrukerRolle {
        MOR, FAR, MEDMOR, UKJENT
    }

    public record Rettigheter(boolean aleneomsorg, boolean morUføretrygd, boolean annenForelderTilsvarendeRettEØS) {
    }

    @Override
    public String toString() {
        return "FpSak{" + "saksnummer='" + saksnummer + '\'' + ", familieHendelse=" + familieHendelse + ", avsluttet=" + avsluttet + ", vedtak="
            + vedtak + ", aksjonspunkt=" + aksjonspunkt + ", søknader=" + søknader + ", brukerRolle=" + brukerRolle + ", rettigheter="
            + rettigheter + '}';
    }

    public record Gradering(Prosent prosent, UttakAktivitet uttakAktivitet) {
    }
}
