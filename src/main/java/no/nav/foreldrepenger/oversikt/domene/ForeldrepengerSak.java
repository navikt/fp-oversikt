package no.nav.foreldrepenger.oversikt.domene;

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
}