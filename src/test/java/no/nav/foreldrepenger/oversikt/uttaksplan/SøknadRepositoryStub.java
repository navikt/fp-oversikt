package no.nav.foreldrepenger.oversikt.uttaksplan;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.søknad.Søknad;
import no.nav.foreldrepenger.oversikt.søknad.SøknadRepository;

class SøknadRepositoryStub implements SøknadRepository {

    private final Map<Saksnummer, Søknad> søknader = new HashMap<>();

    @Override
    public void lagre(Saksnummer saksnummer, Søknad søknad) {
        søknader.put(saksnummer, søknad);
    }

    @Override
    public Optional<Søknad> hentAktivFor(Saksnummer saksnummer) {
        return Optional.ofNullable(søknader.get(saksnummer));
    }
}
