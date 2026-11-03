package no.nav.foreldrepenger.oversikt.stub;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.soknadsplan.Søknadsplan;
import no.nav.foreldrepenger.oversikt.soknadsplan.SøknadsplanRepository;

public class SøknadsplanRepositoryStub implements SøknadsplanRepository {

    private final Map<Saksnummer, Søknadsplan> søknader = new HashMap<>();

    @Override
    public void lagre(Saksnummer saksnummer, Søknadsplan søknad) {
        søknader.put(saksnummer, søknad);
    }

    @Override
    public Optional<Søknadsplan> hentAktivFor(Saksnummer saksnummer) {
        return Optional.ofNullable(søknader.get(saksnummer));
    }
}
