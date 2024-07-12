package no.nav.foreldrepenger.oversikt.stub;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.FeilKode;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.ManglerTilgangException;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontroll;

public record TilgangKontrollStub(boolean harAdresseBeskyttelse,
                                  boolean sakKoblet,
                                  boolean erMyndig,
                                  boolean erBorger,
                                  boolean ukjentIdent,
                                  boolean erSaksbehandlerMedDrift) implements TilgangKontroll {


    public static TilgangKontrollStub beskyttetAdresse() {
        return new TilgangKontrollStub(true, true, true, true, false, false);
    }

    public static TilgangKontroll borger(boolean erMyndig) {
        return new TilgangKontrollStub(false, true, erMyndig, true, false, false);
    }


    public static TilgangKontroll saksbehandler(boolean harDriftrolle) {
        if (harDriftrolle) {
            return new TilgangKontrollStub(false, false, true, false, false, true);
        }
        return new TilgangKontrollStub(false, false, true, false, false, false);

    }

    public static TilgangKontroll ukjentFnr() {
        return new TilgangKontrollStub(true, true, true, true, true, false);
    }

    @Override
    public void tilgangssjekkMyndighetsalder() {
        if (!erMyndig) {
            throw new ManglerTilgangException(FeilKode.IKKE_TILGANG_UMYNDIG);
        }
    }

    @Override
    public boolean erUkjentPerson(String ident) {
        return ukjentIdent;
    }

    @Override
    public boolean harAdresseBeskyttelse(String ident) {
        return harAdresseBeskyttelse;
    }

    @Override
    public void sakKobletTilAktørGuard(Saksnummer saksnummer) {
        if (!erMyndig) {
            throw new ManglerTilgangException(FeilKode.IKKE_TILGANG);
        }
    }

    @Override
    public void sjekkAtKallErFraBorger() {
        if (!erBorger) {
            throw new ManglerTilgangException(FeilKode.IKKE_TILGANG);
        }
    }

    @Override
    public void sjekkAtSaksbehandlerHarRollenDrift() {
        if (!erSaksbehandlerMedDrift) {
            throw new ManglerTilgangException(FeilKode.IKKE_TILGANG);
        }
    }

    @Override
    public Fødselsnummer fødselsnummer(AktørId aktørId) {
        return new Fødselsnummer(aktørId.value());
    }
}
