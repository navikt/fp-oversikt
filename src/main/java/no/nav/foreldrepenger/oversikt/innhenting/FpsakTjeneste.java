package no.nav.foreldrepenger.oversikt.innhenting;

import java.util.UUID;

public interface FpsakTjeneste {
    Sak hentSak(UUID behandlingUuid);

}
