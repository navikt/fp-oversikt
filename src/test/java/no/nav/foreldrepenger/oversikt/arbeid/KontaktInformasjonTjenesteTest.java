package no.nav.foreldrepenger.oversikt.arbeid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.common.domain.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.integrasjoner.digdir.Kontaktinformasjoner;
import no.nav.foreldrepenger.oversikt.integrasjoner.digdir.KrrSpråkKlientSystem;

@ExtendWith(MockitoExtension.class)
class KontaktInformasjonTjenesteTest {

    private static final Fødselsnummer DUMMY_FNR = new Fødselsnummer("12345678901");

    @Mock
    private KrrSpråkKlientSystem krrSpråkKlient;

    private KontaktInformasjonTjeneste kontaktInformasjonTjeneste;

    @BeforeEach
    void setUp() {
        kontaktInformasjonTjeneste = new KontaktInformasjonTjeneste(krrSpråkKlient);
    }

    @Test
    void tom_response_indikerer_feil_og_skal_returener_at_mor_ikke_kan_varles() {
        when(krrSpråkKlient.hentKontaktinformasjon(any())).thenReturn(Optional.empty());

        var harReservertSegEllerKanIkkeVarsles = kontaktInformasjonTjeneste.harReservertSegEllerKanIkkeVarsles(DUMMY_FNR);

        assertThat(harReservertSegEllerKanIkkeVarsles).isTrue();
    }

    @Test
    void ikke_aktiv_kontaktinfomasjon_skal_likebehandles_som_tilfeller_der_mor_ikke_kan_varlses() {
        when(krrSpråkKlient.hentKontaktinformasjon(any())).thenReturn(Optional.of(new Kontaktinformasjoner.Kontaktinformasjon(false, true, false, "NB")));

        var harReservertSegEllerKanIkkeVarsles = kontaktInformasjonTjeneste.harReservertSegEllerKanIkkeVarsles(DUMMY_FNR);

        assertThat(harReservertSegEllerKanIkkeVarsles).isTrue();
    }

    @Test
    void person_kan_varleses_men_har_reservert_seg_og_skal_derfor_ikke_varsles() {
        when(krrSpråkKlient.hentKontaktinformasjon(any())).thenReturn(Optional.of(new Kontaktinformasjoner.Kontaktinformasjon(true, true, true, "NB")));

        var harReservertSegEllerKanIkkeVarsles = kontaktInformasjonTjeneste.harReservertSegEllerKanIkkeVarsles(DUMMY_FNR);

        assertThat(harReservertSegEllerKanIkkeVarsles).isTrue();
    }

    @Test
    void person_som_kan_varsles_og_er_ikke_reservert() {
        when(krrSpråkKlient.hentKontaktinformasjon(any())).thenReturn(Optional.of(new Kontaktinformasjoner.Kontaktinformasjon(true, true, false, "NB")));

        var harReservertSegEllerKanIkkeVarsles = kontaktInformasjonTjeneste.harReservertSegEllerKanIkkeVarsles(DUMMY_FNR);

        assertThat(harReservertSegEllerKanIkkeVarsles).isFalse();
    }
}
