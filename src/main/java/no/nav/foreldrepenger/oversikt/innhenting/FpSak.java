package no.nav.foreldrepenger.oversikt.innhenting;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;
import no.nav.foreldrepenger.oversikt.domene.Prosent;
import no.nav.foreldrepenger.oversikt.domene.Trekkdager;


public record FpSak(String saksnummer,
                    String aktørId,
                    FamilieHendelse familieHendelse,
                    Status status,
                    Set<Vedtak> vedtak,
                    String oppgittAnnenPart,
                    Set<Aksjonspunkt> aksjonspunkt,
                    Set<Søknad> søknader,
                    BrukerRolle brukerRolle,
                    Set<String> fødteBarn,
                    Rettigheter rettigheter) implements Sak {

    public record Vedtak(LocalDateTime vedtakstidspunkt, List<Uttaksperiode> uttaksperioder, Dekningsgrad dekningsgrad) {
        public enum Dekningsgrad {
            ÅTTI,
            HUNDRE
        }
    }

    public record Uttaksperiode(LocalDate fom, LocalDate tom, Resultat resultat) {

        public record Resultat(Type type, Set<UttaksperiodeAktivitet> aktiviteter) {

            public boolean innvilget() {
                return Objects.equals(type, Type.INNVILGET);
            }

            public enum Type {
                INNVILGET,
                AVSLÅTT
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
    public record Søknad(SøknadStatus status, LocalDateTime mottattTidspunkt, Set<Periode> perioder) {


        public record Periode(LocalDate fom, LocalDate tom, Konto konto, UtsettelseÅrsak utsettelseÅrsak, OppholdÅrsak oppholdÅrsak,
                              OverføringÅrsak overføringÅrsak, Gradering gradering, Prosent samtidigUttak, Boolean flerbarnsdager,
                              MorsAktivitet morsAktivitet) {

        }
    }

    public enum BrukerRolle {
        MOR,
        FAR,
        MEDMOR
    }

    public record Rettigheter(boolean aleneomsorg, boolean morUføretrygd, boolean annenForelderTilsvarendeRettEØS) {
    }

    @Override
    public String toString() {
        return "FpSak{" + "saksnummer='" + saksnummer + '\'' + ", familieHendelse=" + familieHendelse + ", status=" + status + ", vedtak="
            + vedtak + ", aksjonspunkt=" + aksjonspunkt + ", søknader=" + søknader + ", brukerRolle=" + brukerRolle + ", rettigheter="
            + rettigheter + '}';
    }

    public record Gradering(Prosent prosent, UttakAktivitet uttakAktivitet) {
    }
}
