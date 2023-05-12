package no.nav.foreldrepenger.oversikt.innhenting;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

public interface FpsakTjeneste {
    Sak hentSak(Saksnummer saksnummer);

}
