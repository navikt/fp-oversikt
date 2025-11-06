package no.nav.foreldrepenger.oversikt.domene.vedlegg.manglende;

import java.util.List;

import no.nav.foreldrepenger.oversikt.arkiv.DokumentTypeHistoriske;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

public interface ManglendeVedleggRepository {

    void lagreManglendeVedleggPåSak(Saksnummer saksnummer, List<DokumentTypeHistoriske> manglendeVedlegg);

    List<DokumentTypeHistoriske> hentFor(Saksnummer saksnummer);

    void slett(Saksnummer saksnummer);
}
