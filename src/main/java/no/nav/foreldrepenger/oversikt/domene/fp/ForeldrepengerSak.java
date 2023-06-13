package no.nav.foreldrepenger.oversikt.domene.fp;

import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.Sak;

import java.util.Optional;
import java.util.Set;

public interface ForeldrepengerSak extends Sak {

    Optional<FpVedtak> gjeldendeVedtak();

    FamilieHendelse familieHendelse();

    Dekningsgrad dekningsgrad();

    Optional<FpSøknad> sisteSøknad();

    AktørId annenPartAktørId();

    Set<AktørId> fødteBarn();

    boolean oppgittAleneomsorg();

    default boolean gjelderBarn(AktørId barnAktørid) {
        return fødteBarn() != null && fødteBarn().contains(barnAktørid);
    }

    @Override
    default boolean harVedtak() {
        return gjeldendeVedtak().isPresent();
    }
}
