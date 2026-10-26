package no.nav.foreldrepenger.oversikt.stub;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.søknadsuttaksplan.Søknadsuttaksplan;
import no.nav.foreldrepenger.oversikt.søknadsuttaksplan.SøknadsuttaksplanRepository;

public class SøknadsuttaksplanRepositoryStub implements SøknadsuttaksplanRepository {

    private final Map<Saksnummer, Søknadsuttaksplan> søknader = new HashMap<>();

    @Override
    public void lagre(Saksnummer saksnummer, Søknadsuttaksplan søknad) {
        søknader.put(saksnummer, søknad);
    }

    @Override
    public Optional<Søknadsuttaksplan> hentAktivFor(Saksnummer saksnummer) {
        return Optional.ofNullable(søknader.get(saksnummer));
    }
}
