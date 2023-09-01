package no.nav.foreldrepenger.oversikt.innhenting;

import no.nav.foreldrepenger.common.domain.felles.DokumentType;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

import java.util.List;

public interface FpsakTjeneste {
    Sak hentSak(Saksnummer saksnummer);
    List<DokumentType> hentMangelendeVedlegg(Saksnummer saksnummer);

}
