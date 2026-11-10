package no.nav.foreldrepenger.oversikt.saker;

import java.util.Optional;

import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.AdresseBeskyttelse;

public interface PersonOppslagSystem {
    /**
     * @throws BrukerIkkeFunnetIPdlException når identen er ukjent i PDL. Tekniske feil mot PDL kastes som unchecked unntak.
     */
    AktørId aktørId(Fødselsnummer fnr) throws BrukerIkkeFunnetIPdlException;

    Fødselsnummer fødselsnummer(AktørId aktørId);

    default Optional<Fødselsnummer> fødselsnummerSjekkBeskyttelse(AktørId aktørId) {
        var fnr = fødselsnummer(aktørId);
        try {
            var beskyttelse = adresseBeskyttelse(fnr);
            if (beskyttelse.harBeskyttetAdresse()) {
                return Optional.empty();
            }
            return Optional.of(fnr);
        } catch (BrukerIkkeFunnetIPdlException _) {
            return Optional.empty();
        }
    }

    AdresseBeskyttelse adresseBeskyttelse(Fødselsnummer fnr) throws BrukerIkkeFunnetIPdlException;

    String navn(String ident);

    boolean barnHarDisseForeldrene(Fødselsnummer barn, Fødselsnummer mor, Fødselsnummer annenForelder);
}
