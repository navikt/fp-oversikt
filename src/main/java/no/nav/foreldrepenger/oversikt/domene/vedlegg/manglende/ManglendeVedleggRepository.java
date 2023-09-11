package no.nav.foreldrepenger.oversikt.domene.vedlegg.manglende;

import java.util.List;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.DokumentTypeId;

public interface ManglendeVedleggRepository {

    void lagreManglendeVedleggPåSak(Saksnummer saksnummer, List<DokumentTypeId> manglendeVedlegg);

    List<DokumentTypeId> hentFor(Saksnummer saksnummer);

    void slett(Saksnummer saksnummer);
}
