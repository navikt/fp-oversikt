package no.nav.foreldrepenger.oversikt.saker;

import no.nav.foreldrepenger.common.domain.felles.DokumentType;
import no.nav.foreldrepenger.common.innsyn.BrukerRolle;
import no.nav.foreldrepenger.common.innsyn.RettighetType;
import no.nav.foreldrepenger.oversikt.arkiv.EnkelJournalpostSelvbetjening;
import no.nav.foreldrepenger.oversikt.arkiv.JournalpostType;
import no.nav.foreldrepenger.oversikt.arkiv.SafSelvbetjeningTjeneste;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static no.nav.foreldrepenger.oversikt.stub.DummyInnloggetTestbruker.myndigInnloggetBruker;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SakerRestErOppdatertTest {

    private static final String FAKE_SAKSNUMMER = "123";
    private final String FAKE_SAKSNUMMER_2 = "987654321";

    @Mock
    private Saker saker;
    @Mock
    private SafSelvbetjeningTjeneste safSelvbetjeningTjeneste;
    private SakerRest sakerRest;


    @BeforeEach
    void initializeKontekst() {
        sakerRest = new SakerRest(saker, safSelvbetjeningTjeneste,  myndigInnloggetBruker(), mock(TilgangKontrollTjeneste.class));
    }

    @Test
    void sakErOppdatertHvisOppdateringstidspunktErEtterMottattidspunktetTilJournalposten() {
        when(safSelvbetjeningTjeneste.alleJournalposter(any())).thenReturn(List.of(arkivertJournalpost(DokumentType.I000001, LocalDateTime.now().minusMinutes(1), FAKE_SAKSNUMMER)));
        when(saker.hent()).thenReturn(new no.nav.foreldrepenger.common.innsyn.Saker(Set.of(fpsak(FAKE_SAKSNUMMER, LocalDateTime.now())), Set.of(), Set.of()));

        assertThat(sakerRest.erSakOppdatertEtterMottattSøknad()).isTrue();
    }

    @Test
    void sakErIkkeOppdatertHvisOppdatertTidspunktetErFørInnsendingstidspunkt() {
        when(safSelvbetjeningTjeneste.alleJournalposter(any())).thenReturn(List.of(arkivertJournalpost(DokumentType.I000001, LocalDateTime.now().minusMinutes(1), FAKE_SAKSNUMMER)));
        when(saker.hent()).thenReturn(new no.nav.foreldrepenger.common.innsyn.Saker(Set.of(fpsak(FAKE_SAKSNUMMER, LocalDateTime.now().minusHours(1))), Set.of(), Set.of()));

        assertThat(sakerRest.erSakOppdatertEtterMottattSøknad()).isFalse();
    }

    @Test
    void ingenSakerIFpoversiktMenArkivertSøknadSkalReturnereFalse() {
        when(safSelvbetjeningTjeneste.alleJournalposter(any())).thenReturn(List.of(arkivertJournalpost(DokumentType.I000001, LocalDateTime.now().minusMinutes(1), FAKE_SAKSNUMMER)));
        when(saker.hent()).thenReturn(new no.nav.foreldrepenger.common.innsyn.Saker(Set.of(), Set.of(), Set.of()));

        assertThat(sakerRest.erSakOppdatertEtterMottattSøknad()).isFalse();
    }

    @Test
    void ingenSakerMedLiktSaksnummerIFpoversiktMenArkivertSøknadSkalReturnereFalse() {
        when(safSelvbetjeningTjeneste.alleJournalposter(any())).thenReturn(List.of(
                arkivertJournalpost(DokumentType.I000001, LocalDateTime.now().minusMinutes(20), FAKE_SAKSNUMMER_2),
                arkivertJournalpost(DokumentType.I000001, LocalDateTime.now().minusMinutes(1), FAKE_SAKSNUMMER)
        ));
        when(saker.hent()).thenReturn(new no.nav.foreldrepenger.common.innsyn.Saker(Set.of(fpsak(FAKE_SAKSNUMMER_2, LocalDateTime.now().minusMinutes(15))), Set.of(), Set.of()));

        assertThat(sakerRest.erSakOppdatertEtterMottattSøknad()).isFalse();
    }

    @Test
    void nårSøknadIkkeHarSaksnummerAntarViAtSakenIkkeErOppdatert() {
        when(safSelvbetjeningTjeneste.alleJournalposter(any())).thenReturn(List.of(arkivertJournalpost(DokumentType.I000001, LocalDateTime.now().minusMinutes(20), null)));

        assertThat(sakerRest.erSakOppdatertEtterMottattSøknad()).isFalse();
        verify(saker, never()).hent();
    }

    @Test
    void har_andre_journalførte_dokumenter_men_ingen_søknad_skal_da_være_oppdatert_og_vi_venter_ikke_på_noe_søknad() {
        when(safSelvbetjeningTjeneste.alleJournalposter(any())).thenReturn(
                List.of(
                        arkivertJournalpost(DokumentType.I000039, LocalDateTime.now().minusMinutes(1), FAKE_SAKSNUMMER),
                        arkivertJournalpost(DokumentType.I000067, LocalDateTime.now().minusMinutes(1), FAKE_SAKSNUMMER)
                ));

        assertThat(sakerRest.erSakOppdatertEtterMottattSøknad()).isTrue();
        verify(saker, never()).hent();
    }

    private static EnkelJournalpostSelvbetjening arkivertJournalpost(DokumentType dokumentType, LocalDateTime mottatt, String saksnummer) {
        return new EnkelJournalpostSelvbetjening(
                dokumentType.getTittel(),
                "123456",
                saksnummer,
                JournalpostType.INNGÅENDE_DOKUMENT,
                mottatt,
                dokumentType,
                List.of());
    }

    private no.nav.foreldrepenger.common.innsyn.FpSak fpsak(String saksnummer, LocalDateTime oppdatertTidspunkt) {
        return new no.nav.foreldrepenger.common.innsyn.FpSak(
                new no.nav.foreldrepenger.common.innsyn.Saksnummer(saksnummer),
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                RettighetType.BEGGE_RETT,
                null,
                null,
                null,
                null,
                Set.of(),
                null,
                oppdatertTidspunkt,
                BrukerRolle.MOR);
    }
}
