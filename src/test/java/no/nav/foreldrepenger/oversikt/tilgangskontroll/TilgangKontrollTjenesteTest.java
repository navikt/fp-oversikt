package no.nav.foreldrepenger.oversikt.tilgangskontroll;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.Kontekst;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;

class TilgangKontrollTjenesteTest {

    private static final String SYSTEMKLIENT = "dev-gcp:teamforeldrepenger:klient";

    private SakRepository sakRepository;
    private InnloggetBruker innloggetBruker;
    private TilgangKontrollTjeneste tilgangkontroll;

    @BeforeEach
    void setUp() {
        KontekstHolder.fjernKontekst();
        sakRepository = mock(SakRepository.class);
        innloggetBruker = mock(InnloggetBruker.class);
        tilgangkontroll = new TilgangKontrollTjeneste(sakRepository, innloggetBruker);
    }

    @AfterEach
    void tearDown() {
        KontekstHolder.fjernKontekst();
    }

    @Test
    void internBrukerBlirRejectedAvBorgerSjekk() {
        var kontekst = mock(Kontekst.class);
        when(kontekst.harKontekst()).thenReturn(true);
        when(kontekst.getIdentType()).thenReturn(IdentType.InternBruker);
        KontekstHolder.setKontekst(kontekst);

        assertThatThrownBy(() -> tilgangkontroll.sjekkAtKallErFraBorger()).isExactlyInstanceOf(OversiktManglerTilgangException.class);
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
        assertThatThrownBy(() -> tilgangkontroll.sakKobletTilAktørGuard(saksnummer)).isExactlyInstanceOf(OversiktManglerTilgangException.class);
    }


    @Test
    void skalHiveExceptionVedUmyndig() {
        when(innloggetBruker.erMyndig()).thenReturn(false);
        assertThatThrownBy(() -> tilgangkontroll.tilgangssjekkMyndighetsalder())
            .isExactlyInstanceOf(OversiktManglerTilgangException.class);
    }

    @Test
    void tilgangHvisMynding() {
        when(innloggetBruker.erMyndig()).thenReturn(true);
        assertThatCode(() -> tilgangkontroll.tilgangssjekkMyndighetsalder()).doesNotThrowAnyException();
    }

    @Test
    void systemkallGodtas() {
        KontekstHolder.setKontekst(kontekst(IdentType.Systemressurs));

        assertThatCode(() -> tilgangkontroll.sjekkAtKallErFraSystem()).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = IdentType.class, names = {"Systemressurs"}, mode = EnumSource.Mode.EXCLUDE)
    void andreIdenttyperAvvises(IdentType identType) {
        KontekstHolder.setKontekst(kontekst(identType));

        assertThatThrownBy(() -> tilgangkontroll.sjekkAtKallErFraSystem())
            .isExactlyInstanceOf(OversiktManglerTilgangException.class)
            .extracting("feilkode").isEqualTo(LokalFeilKode.IKKE_TILGANG_IKKE_SYSTEM.name());
    }

    @Test
    void manglendeKontekstAvvises() {
        assertThatThrownBy(() -> tilgangkontroll.sjekkAtKallErFraSystem())
            .isExactlyInstanceOf(OversiktManglerTilgangException.class)
            .extracting("feilkode").isEqualTo(LokalFeilKode.IKKE_TILGANG_IKKE_SYSTEM.name());
    }

    private static RequestKontekst kontekst(IdentType identType) {
        return RequestKontekst.forRequest(SYSTEMKLIENT, "srvklient", identType, null, null, Set.of());
    }
}
