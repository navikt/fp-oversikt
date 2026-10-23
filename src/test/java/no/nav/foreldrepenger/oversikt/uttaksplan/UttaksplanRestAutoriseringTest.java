package no.nav.foreldrepenger.oversikt.uttaksplan;

import static no.nav.foreldrepenger.oversikt.KontekstTestHelper.innloggetBorger;
import static no.nav.foreldrepenger.oversikt.KontekstTestHelper.innloggetSaksbehandlerUtenDrift;
import static no.nav.foreldrepenger.oversikt.stub.DummyInnloggetTestbruker.myndigInnloggetBruker;
import static no.nav.foreldrepenger.oversikt.stub.DummyInnloggetTestbruker.umyndigInnloggetBruker;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.saker.PersonOppslagSystem;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.LokalFeilKode;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.OversiktManglerTilgangException;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

class UttaksplanRestAutoriseringTest {

    private static final LocalDate FAMILIEHENDELSE = LocalDate.of(2026, 10, 1);
    private static final Fødselsnummer ANNEN_PART_FNR = new Fødselsnummer("annen-part");

    @Test
    void uautentisertKallSkalIkkeKunneHenteUttaksplan() {
        KontekstHolder.fjernKontekst();
        var innloggetBruker = myndigInnloggetBruker();
        var tjeneste = mock(UttaksplanTjeneste.class);
        var personOppslag = mock(PersonOppslagSystem.class);
        var rest = rest(tjeneste, innloggetBruker, personOppslag);

        assertThatThrownBy(() -> rest.hent(requestMedAnnenPart()))
            .isExactlyInstanceOf(OversiktManglerTilgangException.class);
        verifyNoInteractions(tjeneste, personOppslag);
    }

    @Test
    void ansattSkalIkkeKunneHenteUttaksplan() {
        innloggetSaksbehandlerUtenDrift();
        var innloggetBruker = myndigInnloggetBruker();
        var tjeneste = mock(UttaksplanTjeneste.class);
        var personOppslag = mock(PersonOppslagSystem.class);
        var rest = rest(tjeneste, innloggetBruker, personOppslag);

        assertThatThrownBy(() -> rest.hent(requestMedAnnenPart()))
            .isExactlyInstanceOf(OversiktManglerTilgangException.class);
        verifyNoInteractions(tjeneste, personOppslag);
    }

    @Test
    void umyndigBorgerSkalIkkeKunneHenteUttaksplan() {
        innloggetBorger();
        var innloggetBruker = umyndigInnloggetBruker();
        var tjeneste = mock(UttaksplanTjeneste.class);
        var personOppslag = mock(PersonOppslagSystem.class);
        var rest = rest(tjeneste, innloggetBruker, personOppslag);

        assertThatThrownBy(() -> rest.hent(requestMedAnnenPart()))
            .isInstanceOf(OversiktManglerTilgangException.class)
            .extracting(e -> ((OversiktManglerTilgangException) e).getFeilkode())
            .isEqualTo(LokalFeilKode.IKKE_TILGANG_UMYNDIG.name());
        verifyNoInteractions(tjeneste, personOppslag);
    }

    @Test
    void skalBrukeInnloggetBrukerSomSøker() {
        innloggetBorger();
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var innloggetBruker = myndigInnloggetBruker(søker);
        var tjeneste = mock(UttaksplanTjeneste.class);
        var personOppslag = mock(PersonOppslagSystem.class);
        when(personOppslag.aktørId(ANNEN_PART_FNR)).thenReturn(annenPart);
        var rest = rest(tjeneste, innloggetBruker, personOppslag);

        rest.hent(requestMedAnnenPart());

        verify(tjeneste).hentFor(søker, annenPart, null, FAMILIEHENDELSE);
    }

    @Test
    void adressekontrollSkalOverlatesTilTjenesten() throws Exception {
        innloggetBorger();
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var innloggetBruker = myndigInnloggetBruker(søker);
        var tjeneste = mock(UttaksplanTjeneste.class);
        var personOppslag = mock(PersonOppslagSystem.class);
        var forventet = tomPlan();
        when(personOppslag.aktørId(ANNEN_PART_FNR)).thenReturn(annenPart);
        when(tjeneste.hentFor(søker, annenPart, null, FAMILIEHENDELSE)).thenReturn(java.util.Optional.of(forventet));
        var rest = rest(tjeneste, innloggetBruker, personOppslag);

        assertThat(rest.hent(requestMedAnnenPart())).isSameAs(forventet);
        verify(tjeneste).hentFor(søker, annenPart, null, FAMILIEHENDELSE);
        verify(personOppslag, never()).adresseBeskyttelse(ANNEN_PART_FNR);
    }

    @Test
    void ukjentAnnenPartSkalUtelatesMensSøkersPlanReturneres() {
        innloggetBorger();
        var søker = AktørId.dummy();
        var innloggetBruker = myndigInnloggetBruker(søker);
        var tjeneste = mock(UttaksplanTjeneste.class);
        var personOppslag = mock(PersonOppslagSystem.class);
        var forventet = tomPlan();
        when(personOppslag.aktørId(ANNEN_PART_FNR)).thenThrow(new NoSuchElementException());
        when(tjeneste.hentFor(søker, null, null, FAMILIEHENDELSE)).thenReturn(java.util.Optional.of(forventet));
        var rest = rest(tjeneste, innloggetBruker, personOppslag);

        assertThat(rest.hent(requestMedAnnenPart())).isSameAs(forventet);
        verify(tjeneste).hentFor(søker, null, null, FAMILIEHENDELSE);
        verify(personOppslag).aktørId(ANNEN_PART_FNR);
    }

    @Test
    void tekniskIdentfeilSkalPropageresUtenTjenestekall() {
        innloggetBorger();
        var tjeneste = mock(UttaksplanTjeneste.class);
        var personOppslag = mock(PersonOppslagSystem.class);
        var feil = new IllegalStateException("PDL utilgjengelig");
        when(personOppslag.aktørId(ANNEN_PART_FNR)).thenThrow(feil);
        var rest = rest(tjeneste, myndigInnloggetBruker(), personOppslag);

        assertThatThrownBy(() -> rest.hent(requestMedAnnenPart())).isSameAs(feil);
        verifyNoInteractions(tjeneste);
    }

    @Test
    void manglendeAnnenPartSkalSendesVidereSomUavklart() {
        innloggetBorger();
        var søker = AktørId.dummy();
        var tjeneste = mock(UttaksplanTjeneste.class);
        var personOppslag = mock(PersonOppslagSystem.class);
        var rest = rest(tjeneste, myndigInnloggetBruker(søker), personOppslag);

        rest.hent(new UttaksplanRest.FellesUttaksplanRequest(new UttaksplanRest.BarnIdentifikator(null, FAMILIEHENDELSE), null));

        verify(tjeneste).hentFor(søker, null, null, FAMILIEHENDELSE);
        verifyNoInteractions(personOppslag);
    }

    private static UttaksplanRest rest(UttaksplanTjeneste tjeneste,
                                       no.nav.foreldrepenger.oversikt.saker.InnloggetBruker innloggetBruker,
                                       PersonOppslagSystem personOppslag) {
        return new UttaksplanRest(tjeneste, new TilgangKontrollTjeneste(null, innloggetBruker), innloggetBruker, personOppslag);
    }

    private static UttaksplanRest.FellesUttaksplanRequest requestMedAnnenPart() {
        return new UttaksplanRest.FellesUttaksplanRequest(new UttaksplanRest.BarnIdentifikator(null, FAMILIEHENDELSE), ANNEN_PART_FNR);
    }

    private static FellesUttaksplanDto tomPlan() {
        return new FellesUttaksplanDto(FAMILIEHENDELSE, 1, FellesUttaksplanDto.Dekningsgrad.HUNDRE, List.of());
    }
}
