package no.nav.foreldrepenger.oversikt.domene;


import java.util.List;

public interface SakRepository {
    void lagre(Sak sak);

    List<Sak> hentFor(AktørId aktørId);

    boolean erSakKobletTilAktør(Saksnummer saksnummer, AktørId aktørId);
}
