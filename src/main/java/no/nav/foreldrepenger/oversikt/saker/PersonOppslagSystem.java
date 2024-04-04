package no.nav.foreldrepenger.oversikt.saker;

import java.util.Optional;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.AdresseBeskyttelse;

public interface PersonOppslagSystem {
    AktørId aktørId(Fødselsnummer fnr);

    Fødselsnummer fødselsnummer(AktørId aktørId);

    default Optional<Fødselsnummer> fødselsnummerSjekkBeskyttelse(AktørId aktørId) {
        var fnr = fødselsnummer(aktørId);
        try {
            var beskyttelse = adresseBeskyttelse(fnr);
            if (beskyttelse.harBeskyttetAdresse()) {
                return Optional.empty();
            }
            return Optional.of(fnr);
        } catch (BrukerIkkeFunnetIPdlException e) {
            return Optional.empty();
        }
    }

    AdresseBeskyttelse adresseBeskyttelse(Fødselsnummer fnr) throws BrukerIkkeFunnetIPdlException;
}
