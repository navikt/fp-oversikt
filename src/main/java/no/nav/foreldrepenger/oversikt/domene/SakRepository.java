package no.nav.foreldrepenger.oversikt.domene;

import no.nav.foreldrepenger.common.domain.felles.DokumentType;

import java.util.List;

public interface SakRepository {
    void lagre(Sak sak);

    void lagreManglendeVedleggPåSak(String saksnummer, List<DokumentType> manglendeVedlegg);

    List<Sak> hentFor(AktørId aktørId);


}
