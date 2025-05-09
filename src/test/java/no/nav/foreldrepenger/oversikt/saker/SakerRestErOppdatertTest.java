package no.nav.foreldrepenger.oversikt.saker;

import static no.nav.foreldrepenger.oversikt.stub.DummyInnloggetTestbruker.myndigInnloggetBruker;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.common.domain.felles.DokumentType;
import no.nav.foreldrepenger.oversikt.arkiv.EnkelJournalpostSelvbetjening;
import no.nav.foreldrepenger.oversikt.arkiv.JournalpostType;
import no.nav.foreldrepenger.oversikt.arkiv.SafSelvbetjeningTjeneste;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.Sak;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.SøknadStatus;
import no.nav.foreldrepenger.oversikt.domene.fp.Dekningsgrad;
import no.nav.foreldrepenger.oversikt.domene.fp.FpSøknad;
import no.nav.foreldrepenger.oversikt.domene.fp.FpSøknadsperiode;
import no.nav.foreldrepenger.oversikt.domene.fp.Konto;
import no.nav.foreldrepenger.oversikt.domene.fp.Rettigheter;
import no.nav.foreldrepenger.oversikt.domene.fp.SakFP0;
import no.nav.foreldrepenger.oversikt.stub.RepositoryStub;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;

@ExtendWith(MockitoExtension.class)
class SakerRestErOppdatertTest {

    private static final String FAKE_SAKSNUMMER = "123";
    private final String FAKE_SAKSNUMMER_2 = "987654321";

    @Mock
    private SafSelvbetjeningTjeneste safSelvbetjeningTjeneste;

    private SakRepository sakRepository;
    private InnloggetBruker innloggetBruker = myndigInnloggetBruker();
    private SakerRest sakerRest;


    @BeforeEach
    void initializeKontekst() {
        sakRepository = new RepositoryStub();
        var saker = new Saker(sakRepository, innloggetBruker, mock(PersonOppslagSystem.class));
        sakerRest = new SakerRest(saker, safSelvbetjeningTjeneste, innloggetBruker, mock(TilgangKontrollTjeneste.class));
    }

    @Test
    void sakErOppdatertHvisOppdateringstidspunktErEtterMottattidspunktetTilJournalposten() {
        when(safSelvbetjeningTjeneste.alleJournalposter(any())).thenReturn(List.of(arkivertJournalpost(DokumentType.I000001, LocalDateTime.now().minusMinutes(1), FAKE_SAKSNUMMER)));
        sakRepository.lagre(fpsak(FAKE_SAKSNUMMER, LocalDateTime.now(), LocalDateTime.now().minusHours(1)));

        assertThat(sakerRest.erSakOppdatertEtterMottattSøknad()).isTrue();
    }

    @Test
    void sakErIkkeOppdatertHvisOppdatertTidspunktetErFørInnsendingstidspunkt() {
        when(safSelvbetjeningTjeneste.alleJournalposter(any())).thenReturn(List.of(arkivertJournalpost(DokumentType.I000001, LocalDateTime.now().minusMinutes(1), FAKE_SAKSNUMMER)));
        sakRepository.lagre(fpsak(FAKE_SAKSNUMMER, LocalDateTime.now().minusHours(1), LocalDateTime.now()));

        assertThat(sakerRest.erSakOppdatertEtterMottattSøknad()).isFalse();
    }

    @Test
    void ingenSakerIFpoversiktMenArkivertSøknadSkalReturnereFalse() {
        when(safSelvbetjeningTjeneste.alleJournalposter(any())).thenReturn(List.of(arkivertJournalpost(DokumentType.I000001, LocalDateTime.now().minusMinutes(1), FAKE_SAKSNUMMER)));

        assertThat(sakerRest.erSakOppdatertEtterMottattSøknad()).isFalse();
    }

    @Test
    void ingenSakerMedLiktSaksnummerIFpoversiktMenArkivertSøknadSkalReturnereFalse() {
        when(safSelvbetjeningTjeneste.alleJournalposter(any())).thenReturn(List.of(
                arkivertJournalpost(DokumentType.I000001, LocalDateTime.now().minusMinutes(20), FAKE_SAKSNUMMER_2),
                arkivertJournalpost(DokumentType.I000001, LocalDateTime.now().minusMinutes(1), FAKE_SAKSNUMMER)
        ));
        sakRepository.lagre(fpsak(FAKE_SAKSNUMMER_2, LocalDateTime.now().minusMinutes(15), LocalDateTime.now()));

        assertThat(sakerRest.erSakOppdatertEtterMottattSøknad()).isFalse();
    }

    @Test
    void nårSøknadIkkeHarSaksnummerAntarViAtSakenIkkeErOppdatert() {
        when(safSelvbetjeningTjeneste.alleJournalposter(any())).thenReturn(List.of(arkivertJournalpost(DokumentType.I000001, LocalDateTime.now().minusMinutes(20), null)));

        assertThat(sakerRest.erSakOppdatertEtterMottattSøknad()).isFalse();
    }

    @Test
    void har_andre_journalførte_dokumenter_men_ingen_søknad_skal_da_være_oppdatert_og_vi_venter_ikke_på_noe_søknad() {
        when(safSelvbetjeningTjeneste.alleJournalposter(any())).thenReturn(
                List.of(
                        arkivertJournalpost(DokumentType.I000039, LocalDateTime.now().minusMinutes(1), FAKE_SAKSNUMMER),
                        arkivertJournalpost(DokumentType.I000067, LocalDateTime.now().minusMinutes(1), FAKE_SAKSNUMMER)
                ));

        assertThat(sakerRest.erSakOppdatertEtterMottattSøknad()).isTrue();
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

    private Sak fpsak(String saksnummer, LocalDateTime oppdatertTidspunkt, LocalDateTime søknadMottattTidspunkt) {
        var søknadsperiode = new FpSøknadsperiode(LocalDate.now().minusMonths(1), LocalDate.now(), Konto.FELLESPERIODE, null, null, null, null, null, null, null);
        var søknad = new FpSøknad(SøknadStatus.MOTTATT, LocalDateTime.now(), Set.of(søknadsperiode), Dekningsgrad.HUNDRE, false);
        return new SakFP0(
            new Saksnummer(saksnummer),
            innloggetBruker.aktørId(),
            false,
            null,
            null,
            new FamilieHendelse(LocalDate.now().minusMonths(1), LocalDate.now(), 1, null),
            null,
            Set.of(søknad),
            no.nav.foreldrepenger.oversikt.domene.fp.BrukerRolle.MOR,
            null,
            new Rettigheter(false, false, false),
            false,
            oppdatertTidspunkt);
    }
}
