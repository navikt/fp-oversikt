package no.nav.foreldrepenger.oversikt.stub;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import no.nav.foreldrepenger.oversikt.innhenting.FpsakTjeneste;

public class FpsakTjenesteStub implements FpsakTjeneste {

    private final Map<UUID, SakDto> saker;

    public FpsakTjenesteStub(Map<UUID, SakDto> saker) {
        this.saker = new ConcurrentHashMap<>(saker);
    }

    @Override
    public SakDto hentSak(UUID behandlingUuid) {
        return saker.get(behandlingUuid);
    }
}
