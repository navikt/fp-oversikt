package no.nav.foreldrepenger.oversikt.tilgangskontroll;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.PdpRequestBuilder;
import no.nav.vedtak.sikkerhet.abac.pdp.AppRessursData;

/**
 * Ressursdata for endepunkter beskyttet av fp-felles ABAC.
 */
@ApplicationScoped
public class PdpRequestBuilderImpl implements PdpRequestBuilder {

    @Override
    public AppRessursData lagAppRessursData(AbacDataAttributter dataAttributter) {
        return AppRessursData.builder().build();
    }
}
