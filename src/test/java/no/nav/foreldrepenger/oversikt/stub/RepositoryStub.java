package no.nav.foreldrepenger.oversikt.stub;

import java.util.ArrayList;
import java.util.List;

import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Sak;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;

public class RepositoryStub implements SakRepository {

    private final List<Sak> sakList = new ArrayList<>();

    @Override
    public void lagre(Sak sak) {
        this.sakList.add(sak);
    }

    @Override
    public List<Sak> hentFor(AktørId aktørId) {
        return sakList.stream().filter(v -> v.aktørId().equals(aktørId)).distinct().toList();
    }
}
