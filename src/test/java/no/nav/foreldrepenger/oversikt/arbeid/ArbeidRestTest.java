package no.nav.foreldrepenger.oversikt.arbeid;

import static no.nav.foreldrepenger.oversikt.KontekstTestHelper.innloggetBorger;
import static no.nav.foreldrepenger.oversikt.KontekstTestHelper.innloggetSaksbehandlerUtenDrift;
import static no.nav.foreldrepenger.oversikt.stub.DummyInnloggetTestbruker.myndigInnloggetBruker;
import static no.nav.foreldrepenger.oversikt.stub.DummyInnloggetTestbruker.umyndigInnloggetBruker;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.SøknadStatus;
import no.nav.foreldrepenger.oversikt.domene.fp.BrukerRolle;
import no.nav.foreldrepenger.oversikt.domene.fp.Dekningsgrad;
import no.nav.foreldrepenger.oversikt.domene.fp.FpSøknad;
import no.nav.foreldrepenger.oversikt.domene.fp.FpVedtak;
import no.nav.foreldrepenger.oversikt.domene.fp.Rettigheter;
import no.nav.foreldrepenger.oversikt.domene.fp.SakFP0;
import no.nav.foreldrepenger.oversikt.saker.Saker;
import no.nav.foreldrepenger.oversikt.stub.RepositoryStub;
import no.nav.foreldrepenger.oversikt.saker.AnnenPartSakTjeneste;
import no.nav.foreldrepenger.oversikt.saker.BrukerIkkeFunnetIPdlException;
import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;
import no.nav.foreldrepenger.oversikt.saker.PersonOppslagSystem;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.AdresseBeskyttelse;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.LokalFeilKode;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.OversiktManglerTilgangException;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

/**
 * Svaret fra /arbeid/morDokumentasjon skal ikke avsløre om annen part finnes i PDL.
 */
class ArbeidRestTest {

    private static final Fødselsnummer ANNEN_PART_FNR = new Fødselsnummer("12345678901");
    private static final LocalDate FAMILIEHENDELSE = LocalDate.of(2026, 10, 1);
    private static final Fødselsnummer BARN_FNR = new Fødselsnummer("01012612345");

    private final PersonOppslagSystem personOppslagSystem = mock(PersonOppslagSystem.class);
    private final RepositoryStub sakRepository = new RepositoryStub();
    private final KontaktInformasjonTjeneste kontaktInformasjonTjeneste = mock(KontaktInformasjonTjeneste.class);
    private final AktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste arbeidsforholdTjeneste = mock(AktivitetskravArbeidDokumentasjonsKravArbeidsforholdTjeneste.class);

    private InnloggetBruker innloggetBruker;
    private ArbeidRest rest;

    @BeforeEach
    void setUp() {
        innloggetBorger();
        innloggetBruker = myndigInnloggetBruker();
        rest = rest(innloggetBruker);
    }

    private ArbeidRest rest(InnloggetBruker innloggetBruker) {
        var annenPartSakTjeneste = new AnnenPartSakTjeneste(new Saker(sakRepository, innloggetBruker, personOppslagSystem));
        var aktivitetskravTjeneste = new AktivitetskravMåDokumentereMorsArbeidTjeneste(personOppslagSystem, annenPartSakTjeneste,
            kontaktInformasjonTjeneste, arbeidsforholdTjeneste);
        return new ArbeidRest(new TilgangKontrollTjeneste(null, innloggetBruker), innloggetBruker, personOppslagSystem, null,
            aktivitetskravTjeneste);
    }

    @Test
    void uautentisertKallSkalIkkeSlåOppIPdl() {
        KontekstHolder.fjernKontekst();

        assertThatThrownBy(() -> rest(myndigInnloggetBruker()).trengerDokumentereMorsArbeid(request()))
            .isExactlyInstanceOf(OversiktManglerTilgangException.class);
        verifyNoInteractions(personOppslagSystem);
    }

    @Test
    void ansattSkalIkkeSlåOppIPdl() {
        innloggetSaksbehandlerUtenDrift();

        assertThatThrownBy(() -> rest(myndigInnloggetBruker()).trengerDokumentereMorsArbeid(request()))
            .isExactlyInstanceOf(OversiktManglerTilgangException.class);
        verifyNoInteractions(personOppslagSystem);
    }

    @Test
    void umyndigBorgerSkalIkkeSlåOppIPdl() {
        assertThatThrownBy(() -> rest(umyndigInnloggetBruker()).trengerDokumentereMorsArbeid(request()))
            .isInstanceOf(OversiktManglerTilgangException.class)
            .extracting(e -> ((OversiktManglerTilgangException) e).getFeilkode())
            .isEqualTo(LokalFeilKode.IKKE_TILGANG_UMYNDIG.name());
        verifyNoInteractions(personOppslagSystem);
    }

    /**
     * Kontrolltest: endepunktet kan svare false. Uten denne ville sammenligningstestene under passert også om
     * endepunktet alltid svarte true.
     */
    @Test
    void kjentAnnenPartMedSakVarslingOgArbeidGirFalse() throws BrukerIkkeFunnetIPdlException {
        when(personOppslagSystem.adresseBeskyttelse(ANNEN_PART_FNR)).thenReturn(ugradert());
        var annenPartAktørId = AktørId.dummy();
        var barnAktørId = AktørId.dummy();
        when(personOppslagSystem.aktørId(ANNEN_PART_FNR)).thenReturn(annenPartAktørId);
        when(personOppslagSystem.aktørId(BARN_FNR)).thenReturn(barnAktørId);
        sakRepository.lagre(annenPartsSakMedSøkerSomAnnenPart(annenPartAktørId, barnAktørId));
        when(kontaktInformasjonTjeneste.harReservertSegEllerKanIkkeVarsles(ANNEN_PART_FNR)).thenReturn(false);
        when(arbeidsforholdTjeneste.krevesDokumentasjonForAktivitetskravArbeid(any())).thenReturn(false);

        assertThat(rest.trengerDokumentereMorsArbeid(request())).isFalse();
    }

    @Test
    void tekniskPdlFeilVedAdresseoppslagPropagererOgGirIkkeTrue() throws BrukerIkkeFunnetIPdlException {
        var feil = new IllegalStateException("PDL utilgjengelig");
        when(personOppslagSystem.adresseBeskyttelse(ANNEN_PART_FNR)).thenThrow(feil);

        assertThatThrownBy(() -> rest.trengerDokumentereMorsArbeid(request())).isSameAs(feil);
        verify(personOppslagSystem, never()).aktørId(any());
        verifyNoInteractions(kontaktInformasjonTjeneste, arbeidsforholdTjeneste);
    }

    @Test
    void tekniskPdlFeilVedAktørIdOppslagPropagererOgGirIkkeTrue() throws BrukerIkkeFunnetIPdlException {
        var feil = new IllegalStateException("PDL utilgjengelig");
        when(personOppslagSystem.adresseBeskyttelse(ANNEN_PART_FNR)).thenReturn(ugradert());
        when(personOppslagSystem.aktørId(ANNEN_PART_FNR)).thenThrow(feil);

        assertThatThrownBy(() -> rest.trengerDokumentereMorsArbeid(request())).isSameAs(feil);
        verifyNoInteractions(kontaktInformasjonTjeneste, arbeidsforholdTjeneste);
    }

    @Test
    void ukjentAnnenPartIPdlGirSammeSvarSomKjentAnnenPartUtenSakOgRelasjon() throws BrukerIkkeFunnetIPdlException {
        // Kjent, ugradert annen part uten sak med søker og uten foreldrerelasjon til barnet
        when(personOppslagSystem.adresseBeskyttelse(ANNEN_PART_FNR)).thenReturn(ugradert());
        when(personOppslagSystem.aktørId(ANNEN_PART_FNR)).thenReturn(AktørId.dummy());
        when(personOppslagSystem.aktørId(BARN_FNR)).thenReturn(AktørId.dummy());
        // Ingen saker i sakRepository
        when(personOppslagSystem.barnHarDisseForeldrene(any(), any(), any())).thenReturn(false);
        var kjentUtenRelasjon = rest.trengerDokumentereMorsArbeid(request());

        // Samme fnr, nå ukjent i PDL
        when(personOppslagSystem.adresseBeskyttelse(ANNEN_PART_FNR)).thenThrow(new BrukerIkkeFunnetIPdlException());
        var ukjent = rest.trengerDokumentereMorsArbeid(request());

        assertThat(ukjent).isEqualTo(kjentUtenRelasjon).isTrue();
        verify(kontaktInformasjonTjeneste, never()).harReservertSegEllerKanIkkeVarsles(any());
        verify(arbeidsforholdTjeneste, never()).krevesDokumentasjonForAktivitetskravArbeid(any());
    }

    @Test
    void ukjentAnnenPartIPdlGirSammeSvarSomAnnenPartMedBeskyttetAdresse() throws BrukerIkkeFunnetIPdlException {
        when(personOppslagSystem.adresseBeskyttelse(ANNEN_PART_FNR))
            .thenReturn(new AdresseBeskyttelse(Set.of(AdresseBeskyttelse.Gradering.GRADERT)));
        var beskyttet = rest.trengerDokumentereMorsArbeid(request());

        when(personOppslagSystem.adresseBeskyttelse(ANNEN_PART_FNR)).thenThrow(new BrukerIkkeFunnetIPdlException());
        var ukjent = rest.trengerDokumentereMorsArbeid(request());

        assertThat(ukjent).isEqualTo(beskyttet).isTrue();
        verify(personOppslagSystem, never()).aktørId(any());
    }

    private static AdresseBeskyttelse ugradert() {
        return new AdresseBeskyttelse(Set.of(AdresseBeskyttelse.Gradering.UGRADERT));
    }

    private SakFP0 annenPartsSakMedSøkerSomAnnenPart(AktørId annenPartAktørId, AktørId barnAktørId) {
        var dekningsgrad = Dekningsgrad.HUNDRE;
        var vedtak = new FpVedtak(LocalDateTime.now(), List.of(), dekningsgrad, null, null, null);
        var søknad = new FpSøknad(SøknadStatus.BEHANDLET, LocalDateTime.now(), Set.of(), dekningsgrad, false);
        return new SakFP0(Saksnummer.dummy(), annenPartAktørId, true, Set.of(vedtak), innloggetBruker.aktørId(),
            new FamilieHendelse(FAMILIEHENDELSE, FAMILIEHENDELSE, 1, null), Set.of(), Set.of(søknad), BrukerRolle.MOR, Set.of(barnAktørId),
            new Rettigheter(false, false, false), false, LocalDateTime.now());
    }

    private static ArbeidRest.MorArbeidRequest request() {
        return new ArbeidRest.MorArbeidRequest(ANNEN_PART_FNR, BARN_FNR, FAMILIEHENDELSE,
            List.of(new ArbeidRest.PeriodeRequest(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null)));
    }
}
