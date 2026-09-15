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
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.saker.BrukerIkkeFunnetIPdlException;
import no.nav.foreldrepenger.oversikt.saker.PersonOppslagSystem;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.AdresseBeskyttelse;
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
    void skalBrukeInnloggetBrukerSomSøker() throws BrukerIkkeFunnetIPdlException {
        innloggetBorger();
        var søker = AktørId.dummy();
        var annenPart = AktørId.dummy();
        var innloggetBruker = myndigInnloggetBruker(søker);
        var tjeneste = mock(UttaksplanTjeneste.class);
        var personOppslag = mock(PersonOppslagSystem.class);
        when(personOppslag.adresseBeskyttelse(ANNEN_PART_FNR)).thenReturn(new AdresseBeskyttelse(Set.of()));
        when(personOppslag.aktørId(ANNEN_PART_FNR)).thenReturn(annenPart);
        var rest = rest(tjeneste, innloggetBruker, personOppslag);

        rest.hent(requestMedAnnenPart());

        verify(tjeneste).hentFor(søker, annenPart, null, FAMILIEHENDELSE);
    }

    @Test
    void skjermetAnnenPartSkalUtelatesMensSøkersPlanReturneres() throws BrukerIkkeFunnetIPdlException {
        innloggetBorger();
        var søker = AktørId.dummy();
        var innloggetBruker = myndigInnloggetBruker(søker);
        var tjeneste = mock(UttaksplanTjeneste.class);
        var personOppslag = mock(PersonOppslagSystem.class);
        var forventet = tomPlan();
        when(personOppslag.adresseBeskyttelse(ANNEN_PART_FNR))
            .thenReturn(new AdresseBeskyttelse(Set.of(AdresseBeskyttelse.Gradering.GRADERT)));
        when(tjeneste.hentFor(søker, null, null, FAMILIEHENDELSE)).thenReturn(java.util.Optional.of(forventet));
        var rest = rest(tjeneste, innloggetBruker, personOppslag);

        assertThat(rest.hent(requestMedAnnenPart())).isSameAs(forventet);
        verify(tjeneste).hentFor(søker, null, null, FAMILIEHENDELSE);
        verify(personOppslag, never()).aktørId(ANNEN_PART_FNR);
    }

    @Test
    void ukjentAnnenPartSkalUtelatesMensSøkersPlanReturneres() throws BrukerIkkeFunnetIPdlException {
        innloggetBorger();
        var søker = AktørId.dummy();
        var innloggetBruker = myndigInnloggetBruker(søker);
        var tjeneste = mock(UttaksplanTjeneste.class);
        var personOppslag = mock(PersonOppslagSystem.class);
        var forventet = tomPlan();
        when(personOppslag.adresseBeskyttelse(ANNEN_PART_FNR)).thenThrow(new BrukerIkkeFunnetIPdlException());
        when(tjeneste.hentFor(søker, null, null, FAMILIEHENDELSE)).thenReturn(java.util.Optional.of(forventet));
        var rest = rest(tjeneste, innloggetBruker, personOppslag);

        assertThat(rest.hent(requestMedAnnenPart())).isSameAs(forventet);
        verify(tjeneste).hentFor(søker, null, null, FAMILIEHENDELSE);
        verify(personOppslag, never()).aktørId(ANNEN_PART_FNR);
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
