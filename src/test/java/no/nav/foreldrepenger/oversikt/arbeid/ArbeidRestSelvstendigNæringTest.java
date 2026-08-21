package no.nav.foreldrepenger.oversikt.arbeid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.kontrakter.fpoversikt.SelvstendigNæring;
import no.nav.foreldrepenger.oversikt.integrasjoner.brreg.BrregRollerTjeneste;
import no.nav.foreldrepenger.oversikt.integrasjoner.brreg.BrregSelvstendigNæring;
import no.nav.foreldrepenger.oversikt.integrasjoner.brreg.SNRolleType;
import no.nav.foreldrepenger.oversikt.oppslag.felles.MineArbeidsforholdTjeneste;
import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;
import no.nav.foreldrepenger.oversikt.saker.PersonOppslagSystem;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;

class ArbeidRestSelvstendigNæringTest {

    @Test
    void skalHenteNæringForInnloggetMyndigBorger() {
        var tilgangskontroll = mock(TilgangKontrollTjeneste.class);
        var innloggetBruker = mock(InnloggetBruker.class);
        var brregRollerTjeneste = mock(BrregRollerTjeneste.class);
        var fødselsnummer = new Fødselsnummer("12345678901");
        when(innloggetBruker.fødselsnummer()).thenReturn(fødselsnummer);
        when(brregRollerTjeneste.finnSelvstendigNæring(fødselsnummer)).thenReturn(List.of(
            new BrregSelvstendigNæring(
                "987654321",
                "Mitt foretak",
                "ENK",
                "Enkeltpersonforetak",
                no.nav.foreldrepenger.oversikt.integrasjoner.brreg.Virksomhetstype.FISKE,
                false,
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2020, 2, 1),
                List.of(SNRolleType.INNEHAVER)
            )
        ));
        var rest = new ArbeidRest(
            tilgangskontroll,
            innloggetBruker,
            mock(PersonOppslagSystem.class),
            mock(MineArbeidsforholdTjeneste.class),
            mock(AktivitetskravMåDokumentereMorsArbeidTjeneste.class),
            brregRollerTjeneste
        );

        var resultat = rest.hentSelvstendigNæring();

        assertThat(resultat).containsExactly(new SelvstendigNæring(
            "987654321",
            "Mitt foretak",
            SelvstendigNæring.Virksomhetstype.FISKE
        ));
        verify(tilgangskontroll).sjekkAtKallErFraBorger();
        verify(tilgangskontroll).tilgangssjekkMyndighetsalder();
        verify(brregRollerTjeneste).finnSelvstendigNæring(fødselsnummer);
    }
}
