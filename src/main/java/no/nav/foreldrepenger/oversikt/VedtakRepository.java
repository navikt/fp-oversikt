package no.nav.foreldrepenger.oversikt;

import java.util.List;

public interface VedtakRepository {
    void lagre(Vedtak vedtak);

    List<Vedtak> hentFor(String aktørId);
}
