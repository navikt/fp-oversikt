package no.nav.foreldrepenger.oversikt.stub;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Sak;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

public class RepositoryStub implements SakRepository {

    private final Set<Sak> sakList = new HashSet<>();

    @Override
    public void lagre(Sak sak) {
        this.sakList.add(sak);
    }

    @Override
    public List<Sak> hentFor(AktørId aktørId) {
        return sakList.stream().filter(v -> v.aktørId().equals(aktørId)).toList();
    }

    @Override
    public boolean erSakKobletTilAktør(Saksnummer saksnummer, AktørId aktørId) {
        return sakList.stream().anyMatch(sak -> sak.saksnummer().equals(saksnummer) && sak.aktørId().equals(aktørId));
    }
}
