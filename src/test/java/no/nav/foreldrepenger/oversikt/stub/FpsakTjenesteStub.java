package no.nav.foreldrepenger.oversikt.stub;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import no.nav.foreldrepenger.oversikt.innhenting.FpsakTjeneste;
import no.nav.foreldrepenger.oversikt.innhenting.Sak;

public class FpsakTjenesteStub implements FpsakTjeneste {

    private final Map<UUID, Sak> saker;

    public FpsakTjenesteStub(Map<UUID, Sak> saker) {
        this.saker = new ConcurrentHashMap<>(saker);
    }

    @Override
    public Sak hentSak(UUID behandlingUuid) {
        return saker.get(behandlingUuid);
    }
}
