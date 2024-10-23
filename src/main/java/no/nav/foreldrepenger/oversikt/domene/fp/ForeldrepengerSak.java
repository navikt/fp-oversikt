package no.nav.foreldrepenger.oversikt.domene.fp;

import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.Sak;
import no.nav.foreldrepenger.oversikt.domene.YtelseType;

public interface ForeldrepengerSak extends Sak {

    Optional<FpVedtak> gjeldendeVedtak();

    FamilieHendelse familieHendelse();

    Dekningsgrad dekningsgrad();

    Optional<FpSøknad> sisteSøknad();

    AktørId annenPartAktørId();

    Set<AktørId> fødteBarn();

    boolean oppgittAleneomsorg();

    BrukerRolle brukerRolle();

    default boolean gjelderBarn(AktørId barnAktørid) {
        return fødteBarn() != null && fødteBarn().contains(barnAktørid);
    }

    @Override
    default boolean harVedtak() {
        return gjeldendeVedtak().isPresent();
    }

    @Override
    default YtelseType ytelse() {
        return YtelseType.FORELDREPENGER;
    }
}
