package no.nav.foreldrepenger.oversikt.domene.vedlegg.manglende;

import java.util.List;

import no.nav.foreldrepenger.common.domain.felles.DokumentType;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

public interface ManglendeVedleggRepository {

    void lagreManglendeVedleggPåSak(Saksnummer saksnummer, List<DokumentType> manglendeVedlegg);

    List<DokumentType> hentFor(Saksnummer saksnummer);

    void slett(Saksnummer saksnummer);
}
