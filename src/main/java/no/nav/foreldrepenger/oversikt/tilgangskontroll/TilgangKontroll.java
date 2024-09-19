package no.nav.foreldrepenger.oversikt.tilgangskontroll;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

public interface TilgangKontroll {
    void tilgangssjekkMyndighetsalder();

    boolean erUkjentPerson(String ident);

    boolean harAdresseBeskyttelse(String ident);

    void sakKobletTilAktørGuard(Saksnummer saksnummer);

    void sjekkAtKallErFraBorger();

    void sjekkAtSaksbehandlerHarRollenDrift();

    Fødselsnummer fødselsnummer(AktørId aktørId);
}
