package no.nav.foreldrepenger.oversikt.integrasjoner.brreg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;

class BrregRollerTjenesteTest {

    @Test
    void skalHenteFiltrereOgSortereRegistrerteNæringerUtenforProduksjon() {
        var restClient = mock(RestClient.class);
        var tjeneste = new BrregRollerTjeneste(restClient, RestConfig.forClient(BrregRollerTjeneste.class));
        var rolleutskrift = new BrregRolleutskriftDto(List.of(
            enhet("2", "Zeta", false, false),
            enhet("1", "Alfa", false, false),
            enhet("3", "Tidligere", true, false)
        ));
        when(restClient.sendReturnOptional(any(RestRequest.class), eq(BrregRolleutskriftDto.class)))
            .thenReturn(Optional.of(rolleutskrift));

        var resultat = tjeneste.finnSelvstendigNæring(unikFødselsnummer());

        assertThat(resultat).extracting(BrregSelvstendigNæring::navn).containsExactly("Alfa", "Zeta");
    }

    @Test
    void skalPropagereUventetIntegrasjonsfeil() {
        var restClient = mock(RestClient.class);
        var tjeneste = new BrregRollerTjeneste(restClient, RestConfig.forClient(BrregRollerTjeneste.class));
        when(restClient.sendReturnOptional(any(RestRequest.class), eq(BrregRolleutskriftDto.class)))
            .thenThrow(new IllegalStateException("Brreg er utilgjengelig"));

        assertThatThrownBy(() -> tjeneste.finnSelvstendigNæring(unikFødselsnummer()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Brreg er utilgjengelig");
    }

    private static Fødselsnummer unikFødselsnummer() {
        return new Fødselsnummer(UUID.randomUUID().toString());
    }

    private static BrregRolleutskriftDto.EnhetDto enhet(String organisasjonsnummer,
                                                        String navn,
                                                        boolean fratrådt,
                                                        boolean avregistrert) {
        return new BrregRolleutskriftDto.EnhetDto(
            organisasjonsnummer,
            navn,
            List.of(new BrregRolleutskriftDto.RolleDto(
                fratrådt,
                avregistrert,
                new BrregRolleutskriftDto.RolleKodeDto("INNH", "Innehaver")
            )),
            null
        );
    }
}
