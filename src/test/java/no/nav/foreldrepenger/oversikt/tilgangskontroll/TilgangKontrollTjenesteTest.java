package no.nav.foreldrepenger.oversikt.tilgangskontroll;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.Kontekst;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

class TilgangKontrollTjenesteTest {

    private SakRepository sakRepository;
    private InnloggetBruker innloggetBruker;
    private TilgangKontrollTjeneste tilgangkontroll;

    @BeforeEach
    void setUp() {
        sakRepository = mock(SakRepository.class);
        innloggetBruker = mock(InnloggetBruker.class);
        tilgangkontroll = new TilgangKontrollTjeneste(sakRepository, innloggetBruker);
    }

    @Test
    void internBrukerBlirRejectedAvBorgerSjekk() {
        var kontekst = mock(Kontekst.class);
        when(kontekst.harKontekst()).thenReturn(true);
        when(kontekst.getIdentType()).thenReturn(IdentType.InternBruker);
        KontekstHolder.setKontekst(kontekst);

        assertThatThrownBy(() -> tilgangkontroll.sjekkAtKallErFraBorger()).isExactlyInstanceOf(ManglerTilgangException.class);
    }


    @Test
    void skalGiTilgangTilBorger() {
        var kontekst = mock(Kontekst.class);
        when(kontekst.harKontekst()).thenReturn(true);
        when(kontekst.getIdentType()).thenReturn(IdentType.EksternBruker);
        KontekstHolder.setKontekst(kontekst);

        assertThatCode(() -> tilgangkontroll.sjekkAtKallErFraBorger()).doesNotThrowAnyException();
    }

    @Test
    void tilgangTilSakHvisSakErKoblet() {
        when(innloggetBruker.aktørId()).thenReturn(AktørId.dummy());
        when(sakRepository.erSakKobletTilAktør(any(), any())).thenReturn(true);
        assertThatCode(() -> tilgangkontroll.sakKobletTilAktørGuard(Saksnummer.dummy())).doesNotThrowAnyException();
    }

    @Test
    void skalHiveExceptionHvisSaksnummerIkkeErKobletTilAktørId() {
        var saksnummer = Saksnummer.dummy();
        when(innloggetBruker.aktørId()).thenReturn(AktørId.dummy());
        when(sakRepository.erSakKobletTilAktør(any(), any())).thenReturn(false);
        assertThatThrownBy(() -> tilgangkontroll.sakKobletTilAktørGuard(saksnummer)).isExactlyInstanceOf(ManglerTilgangException.class);
    }


    @Test
    void skalHiveExceptionVedUmyndig() {
        when(innloggetBruker.erMyndig()).thenReturn(false);
        assertThatThrownBy(() -> tilgangkontroll.tilgangssjekkMyndighetsalder())
            .isExactlyInstanceOf(ManglerTilgangException.class);
    }

    @Test
    void tilgangHvisMynding() {
        when(innloggetBruker.erMyndig()).thenReturn(true);
        assertThatCode(() -> tilgangkontroll.tilgangssjekkMyndighetsalder()).doesNotThrowAnyException();
    }
}
