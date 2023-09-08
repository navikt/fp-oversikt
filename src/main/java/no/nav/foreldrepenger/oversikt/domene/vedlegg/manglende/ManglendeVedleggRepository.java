package no.nav.foreldrepenger.oversikt.domene.vedlegg.manglende;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.DokumentType;

import java.util.List;

public interface ManglendeVedleggRepository {

    void lagreManglendeVedleggPåSak(Saksnummer saksnummer, List<DokumentType> manglendeVedlegg);

    List<DokumentType> hentFor(Saksnummer saksnummer);

    void slett(Saksnummer saksnummer);
}
