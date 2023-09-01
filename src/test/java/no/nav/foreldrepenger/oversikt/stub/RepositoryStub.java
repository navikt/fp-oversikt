package no.nav.foreldrepenger.oversikt.stub;

import no.nav.foreldrepenger.common.domain.felles.DokumentType;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Sak;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RepositoryStub implements SakRepository {

    private final Set<Sak> sakList = new HashSet<>();

    @Override
    public void lagre(Sak sak) {
        this.sakList.add(sak);
    }

    @Override
    public void lagreManglendeVedleggPåSak(String saksnummer, List<DokumentType> manglendeVedlegg) {
        // TODO
    }

    @Override
    public List<Sak> hentFor(AktørId aktørId) {
        return sakList.stream().filter(v -> v.aktørId().equals(aktørId)).toList();
    }
}
