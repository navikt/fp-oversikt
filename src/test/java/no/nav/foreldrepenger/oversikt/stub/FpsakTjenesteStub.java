package no.nav.foreldrepenger.oversikt.stub;

import no.nav.foreldrepenger.common.domain.felles.DokumentType;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.innhenting.FpsakTjeneste;
import no.nav.foreldrepenger.oversikt.innhenting.Sak;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FpsakTjenesteStub implements FpsakTjeneste {

    private final Map<Saksnummer, Sak> saker;

    public FpsakTjenesteStub(Map<Saksnummer, Sak> saker) {
        this.saker = new ConcurrentHashMap<>(saker);
    }

    @Override
    public Sak hentSak(Saksnummer saksnummer) {
        return saker.get(saksnummer);
    }

    @Override
    public List<DokumentType> hentMangelendeVedlegg(Saksnummer saksnummer) {
        return null; // TODO
    }
}
