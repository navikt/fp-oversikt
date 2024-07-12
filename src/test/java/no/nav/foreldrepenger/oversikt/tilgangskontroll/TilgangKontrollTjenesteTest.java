package no.nav.foreldrepenger.oversikt.tilgangskontroll;

import static no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangPersondataStub.tilgangpersondata;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.KontekstTestHelper;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.stub.DummyInnloggetTestbruker;

class TilgangKontrollTjenesteTest {

    private static final AktørId AKTØR_ID_DUMMY = AktørId.dummy();
    private SakRepository sakRepository;

    @BeforeEach
    void setUp() {
        sakRepository = mock(SakRepository.class);
    }

    @Test
    void tilgangTilSakHvisSakErKoblet() {
        when(sakRepository.erSakKobletTilAktør(any(), any())).thenReturn(true);
        var tilgangkontroll = new TilgangKontrollTjeneste(sakRepository, tilgangpersondata(AKTØR_ID_DUMMY), DummyInnloggetTestbruker.myndigInnloggetBruker());

        assertThatCode(() -> tilgangkontroll.sakKobletTilAktørGuard(Saksnummer.dummy())).doesNotThrowAnyException();
    }

    @Test
    void skalHiveExceptionHvisSaksnummerIkkeErKobletTilAktørId() {
        when(sakRepository.erSakKobletTilAktør(any(), any())).thenReturn(false);
        var tilgangkontroll = new TilgangKontrollTjeneste(sakRepository, tilgangpersondata(AKTØR_ID_DUMMY), DummyInnloggetTestbruker.myndigInnloggetBruker());

        assertThatThrownBy(() -> tilgangkontroll.sakKobletTilAktørGuard(Saksnummer.dummy())).isExactlyInstanceOf(ManglerTilgangException.class);
    }


    @Test
    void skalHiveExceptionVedUmyndig() {
        var tilgangkontroll = new TilgangKontrollTjeneste(sakRepository, tilgangpersondata(AKTØR_ID_DUMMY, LocalDate.now().minusYears(15)), DummyInnloggetTestbruker.myndigInnloggetBruker());

        assertThatThrownBy(tilgangkontroll::tilgangssjekkMyndighetsalder).isExactlyInstanceOf(ManglerTilgangException.class);
    }

    @Test
    void tilgangHvisMynding() {
        var tilgangkontroll = new TilgangKontrollTjeneste(sakRepository, tilgangpersondata(AKTØR_ID_DUMMY, LocalDate.now().minusYears(19)), DummyInnloggetTestbruker.myndigInnloggetBruker());

        assertThatCode(tilgangkontroll::tilgangssjekkMyndighetsalder).doesNotThrowAnyException();
    }

    @Test
    void ansattMedDriftBlirRejectedAvBorgerSjekkMenIkkeDriftSjekk() {
        KontekstTestHelper.innloggetSaksbehandlerMedDriftRolle();

        var tilgangkontroll = new TilgangKontrollTjeneste(sakRepository, null, null);

        assertThatThrownBy(tilgangkontroll::sjekkAtKallErFraBorger).isExactlyInstanceOf(ManglerTilgangException.class);
        assertThatCode(tilgangkontroll::sjekkAtSaksbehandlerHarRollenDrift).doesNotThrowAnyException();
    }

    @Test
    void ansattUtenDriftSkalIkkeFåTilgang() {
        KontekstTestHelper.innloggetSaksbehandlerUtenDrift();

        var tilgangkontroll = new TilgangKontrollTjeneste(sakRepository, null, null);

        assertThatThrownBy(tilgangkontroll::sjekkAtKallErFraBorger).isExactlyInstanceOf(ManglerTilgangException.class);
        assertThatThrownBy(tilgangkontroll::sjekkAtSaksbehandlerHarRollenDrift).isExactlyInstanceOf(ManglerTilgangException.class);
    }

    @Test
    void skalGiTilgangTilBorgerMenIkkeAnsatt() {
        KontekstTestHelper.innloggetBorger();
        var tilgangkontroll = new TilgangKontrollTjeneste(sakRepository, null, null);

        assertThatCode(tilgangkontroll::sjekkAtKallErFraBorger).doesNotThrowAnyException();
        assertThatThrownBy(tilgangkontroll::sjekkAtSaksbehandlerHarRollenDrift).isExactlyInstanceOf(ManglerTilgangException.class);
    }


}
