package no.nav.foreldrepenger.oversikt.stub;

import java.util.ArrayList;
import java.util.List;

import no.nav.foreldrepenger.oversikt.Vedtak;
import no.nav.foreldrepenger.oversikt.VedtakRepository;

public class RepositoryStub implements VedtakRepository {

    private final List<Vedtak> vedtakList = new ArrayList<>();

    @Override
    public void lagre(Vedtak vedtak) {
        this.vedtakList.add(vedtak);
    }

    @Override
    public List<Vedtak> hentFor(String aktørId) {
        return vedtakList.stream().filter(v -> v.aktørId().equals(aktørId)).distinct().toList();
    }
}
