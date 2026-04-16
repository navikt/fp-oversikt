package no.nav.foreldrepenger.oversikt.tilgangskontroll;

import no.nav.vedtak.exception.ManglerTilgangException;

public class OversiktManglerTilgangException extends ManglerTilgangException {

    private final LokalFeilKode lokalFeilKode;

    public OversiktManglerTilgangException(LokalFeilKode lokalFeilKode, Throwable cause) {
        super(null, lokalFeilKode.getBeskrivelse(), cause);
        this.lokalFeilKode = lokalFeilKode;
    }

    public OversiktManglerTilgangException(LokalFeilKode lokalFeilKode) {
        this(lokalFeilKode, null);
    }

    @Override
    public String getFeilkode() {
        return lokalFeilKode.name();
    }

    public LokalFeilKode getLokalFeilKode() {
        return lokalFeilKode;
    }
}
