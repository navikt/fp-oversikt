package no.nav.foreldrepenger.oversikt.uttaksplan;

import static no.nav.foreldrepenger.oversikt.KontekstTestHelper.innloggetBorger;
import static no.nav.foreldrepenger.oversikt.KontekstTestHelper.innloggetSaksbehandlerUtenDrift;
import static no.nav.foreldrepenger.oversikt.stub.DummyInnloggetTestbruker.myndigInnloggetBruker;
import static no.nav.foreldrepenger.oversikt.stub.DummyInnloggetTestbruker.umyndigInnloggetBruker;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.saker.BrukerIkkeFunnetIPdlException;
import no.nav.foreldrepenger.oversikt.saker.InnloggetBruker;
import no.nav.foreldrepenger.oversikt.saker.PersonOppslagSystem;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.AdresseBeskyttelse;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.LokalFeilKode;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.OversiktManglerTilgangException;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

class UttaksplanRestAutoriseringTest {

    private static final LocalDate FAMILIEHENDELSE = LocalDate.of(2026, 10, 1);
    private static final Fødselsnummer ANNEN_PART_FNR = new Fødselsnummer("12345678901");
    private static final AktørId ANNEN_PART = new AktørId("1234567890123");
    private static final Fødselsnummer BARN_FNR = new Fødselsnummer("01012612345");
    private static final AktørId BARN = new AktørId("9876543210987");
    private static final AdresseBeskyttelse UGRADERT = new AdresseBeskyttelse(Set.of(AdresseBeskyttelse.Gradering.UGRADERT));

    private final RegistrerendeTjeneste tjeneste = new RegistrerendeTjeneste();
    private final PersonOppslagSystem personOppslag = mock(PersonOppslagSystem.class);

    @BeforeEach
    void kjentUgradertAnnenPartOgBarn() throws BrukerIkkeFunnetIPdlException {
        when(personOppslag.aktørId(ANNEN_PART_FNR)).thenReturn(ANNEN_PART);
        when(personOppslag.aktørId(BARN_FNR)).thenReturn(BARN);
        when(personOppslag.adresseBeskyttelse(ANNEN_PART_FNR)).thenReturn(UGRADERT);
    }

    @Test
    void uautentisertKallSkalIkkeKunneHenteUttaksplan() {
        KontekstHolder.fjernKontekst();

        var rest = rest(myndigInnloggetBruker());
        var request = requestMedAnnenPart();

        assertThatThrownBy(() -> rest.hent(request))
            .isExactlyInstanceOf(OversiktManglerTilgangException.class);
        assertThat(tjeneste.kall).isEmpty();
        verifyNoInteractions(personOppslag);
    }

    @Test
    void ansattSkalIkkeKunneHenteUttaksplan() {
        innloggetSaksbehandlerUtenDrift();

        var rest = rest(myndigInnloggetBruker());
        var request = requestMedAnnenPart();

        assertThatThrownBy(() -> rest.hent(request))
            .isExactlyInstanceOf(OversiktManglerTilgangException.class);
        assertThat(tjeneste.kall).isEmpty();
        verifyNoInteractions(personOppslag);
    }

    @Test
    void umyndigBorgerSkalIkkeKunneHenteUttaksplan() {
        innloggetBorger();

        var rest = rest(umyndigInnloggetBruker());
        var request = requestMedAnnenPart();

        assertThatThrownBy(() -> rest.hent(request))
            .isInstanceOf(OversiktManglerTilgangException.class)
            .extracting(e -> ((OversiktManglerTilgangException) e).getFeilkode())
            .isEqualTo(LokalFeilKode.IKKE_TILGANG_UMYNDIG.name());
        assertThat(tjeneste.kall).isEmpty();
        verifyNoInteractions(personOppslag);
    }

    @Test
    void ugradertAnnenPartSendesVerifisertTilTjenestenMedInnloggetBrukerSomSøker() {
        innloggetBorger();
        var søker = AktørId.dummy();

        var plan = rest(myndigInnloggetBruker(søker)).hent(requestMedAnnenPart());

        assertThat(plan).isSameAs(RegistrerendeTjeneste.PLAN);
        assertThat(tjeneste.kall).containsExactly(new Kall(søker, new VerifisertAnnenPart(ANNEN_PART), null));
    }

    private enum SkjultAnnenPart { BESKYTTET, UKJENT_VED_AKTØRID_OPPSLAG, UKJENT_VED_ADRESSEOPPSLAG, MANGLENDE_ADRESSEBESKYTTELSE, IKKE_OPPGITT }

    @ParameterizedTest
    @EnumSource(SkjultAnnenPart.class)
    void skjultAnnenPartSendesSomIkkeOppgittTilTjenesten(SkjultAnnenPart tilfelle) throws BrukerIkkeFunnetIPdlException {
        innloggetBorger();
        var søker = AktørId.dummy();
        switch (tilfelle) {
            case BESKYTTET -> when(personOppslag.adresseBeskyttelse(ANNEN_PART_FNR))
                .thenReturn(new AdresseBeskyttelse(Set.of(AdresseBeskyttelse.Gradering.GRADERT)));
            case UKJENT_VED_AKTØRID_OPPSLAG -> when(personOppslag.aktørId(ANNEN_PART_FNR)).thenThrow(new BrukerIkkeFunnetIPdlException());
            case UKJENT_VED_ADRESSEOPPSLAG -> when(personOppslag.adresseBeskyttelse(ANNEN_PART_FNR)).thenThrow(new BrukerIkkeFunnetIPdlException());
            case MANGLENDE_ADRESSEBESKYTTELSE -> when(personOppslag.adresseBeskyttelse(ANNEN_PART_FNR)).thenReturn(null);
            case IKKE_OPPGITT -> { /* standardoppsett fra @BeforeEach */ }
        }
        var request = tilfelle == SkjultAnnenPart.IKKE_OPPGITT ? requestUtenAnnenPart() : requestMedAnnenPart();

        var plan = rest(myndigInnloggetBruker(søker)).hent(request);

        assertThat(plan).isSameAs(RegistrerendeTjeneste.PLAN);
        assertThat(tjeneste.kall).containsExactly(new Kall(søker, null, null));
    }

    @Test
    void tekniskPdlFeilVedAktørIdOppslagPropagererUtenTjenestekall() throws BrukerIkkeFunnetIPdlException {
        innloggetBorger();
        var feil = new IllegalStateException("PDL utilgjengelig");
        when(personOppslag.aktørId(ANNEN_PART_FNR)).thenThrow(feil);

        var rest = rest(myndigInnloggetBruker());
        var request = requestMedAnnenPart();

        assertThatThrownBy(() -> rest.hent(request)).isSameAs(feil);
        assertThat(tjeneste.kall).isEmpty();
    }

    @Test
    void tekniskPdlFeilVedAdresseoppslagPropagererUtenTjenestekall() throws BrukerIkkeFunnetIPdlException {
        innloggetBorger();
        var feil = new IllegalStateException("PDL utilgjengelig");
        when(personOppslag.adresseBeskyttelse(ANNEN_PART_FNR)).thenThrow(feil);

        var rest = rest(myndigInnloggetBruker());
        var request = requestMedAnnenPart();

        assertThatThrownBy(() -> rest.hent(request)).isSameAs(feil);
        assertThat(tjeneste.kall).isEmpty();
    }

    @Test
    void ukjentBarnIPdlGirSammeSvarSomKjentBarnUtenSak() throws BrukerIkkeFunnetIPdlException {
        innloggetBorger();
        tjeneste.utenSak = true;

        var kjentBarnUtenSak = rest(myndigInnloggetBruker()).hent(requestMedBarnFnr());
        when(personOppslag.aktørId(BARN_FNR)).thenThrow(new BrukerIkkeFunnetIPdlException());
        var ukjentBarn = rest(myndigInnloggetBruker()).hent(requestMedBarnFnr());

        assertThat(kjentBarnUtenSak).isNull();
        assertThat(ukjentBarn).isNull();
        assertThat(tjeneste.kall).extracting(Kall::barn).containsExactly(BARN, null);
    }

    @Test
    void ukjentBarnIPdlGirSammeSvarSomKjentBarnNårFamiliehendelseTrefferSøkersSak() throws BrukerIkkeFunnetIPdlException {
        innloggetBorger();
        var request = new UttaksplanRest.FellesUttaksplanRequest(new UttaksplanRest.BarnIdentifikator(BARN_FNR, FAMILIEHENDELSE), null);

        var kjentBarn = rest(myndigInnloggetBruker()).hent(request);
        when(personOppslag.aktørId(BARN_FNR)).thenThrow(new BrukerIkkeFunnetIPdlException());
        var ukjentBarn = rest(myndigInnloggetBruker()).hent(request);

        assertThat(kjentBarn).isEqualTo(RegistrerendeTjeneste.PLAN);
        assertThat(ukjentBarn).isEqualTo(kjentBarn);
        assertThat(tjeneste.kall).extracting(Kall::barn).containsExactly(BARN, null);
    }

    @Test
    void tekniskPdlFeilForBarnPropagerer() throws BrukerIkkeFunnetIPdlException {
        innloggetBorger();
        var feil = new IllegalStateException("PDL utilgjengelig");
        when(personOppslag.aktørId(BARN_FNR)).thenThrow(feil);

        assertThatThrownBy(() -> rest(myndigInnloggetBruker()).hent(requestMedBarnFnr())).isSameAs(feil);
        assertThat(tjeneste.kall).isEmpty();
    }

    private UttaksplanRest rest(InnloggetBruker innloggetBruker) {
        return new UttaksplanRest(tjeneste, new TilgangKontrollTjeneste(null, innloggetBruker), innloggetBruker, personOppslag);
    }

    private static UttaksplanRest.FellesUttaksplanRequest requestMedAnnenPart() {
        return new UttaksplanRest.FellesUttaksplanRequest(new UttaksplanRest.BarnIdentifikator(null, FAMILIEHENDELSE), ANNEN_PART_FNR);
    }

    private static UttaksplanRest.FellesUttaksplanRequest requestMedBarnFnr() {
        return new UttaksplanRest.FellesUttaksplanRequest(new UttaksplanRest.BarnIdentifikator(BARN_FNR, null), null);
    }

    private static UttaksplanRest.FellesUttaksplanRequest requestUtenAnnenPart() {
        return new UttaksplanRest.FellesUttaksplanRequest(new UttaksplanRest.BarnIdentifikator(null, FAMILIEHENDELSE), null);
    }

    private record Kall(AktørId søker, VerifisertAnnenPart annenPart, AktørId barn) {
    }

    private static class RegistrerendeTjeneste extends UttaksplanTjeneste {
        static final FellesUttaksplanDto PLAN = new FellesUttaksplanDto(FAMILIEHENDELSE, 1, FellesUttaksplanDto.Dekningsgrad.HUNDRE, List.of(), true);
        final List<Kall> kall = new ArrayList<>();
        boolean utenSak;

        @Override
        public Optional<FellesUttaksplanDto> hentFor(AktørId søker, VerifisertAnnenPart annenPart, AktørId barn, LocalDate familiehendelse) {
            kall.add(new Kall(søker, annenPart, barn));
            return utenSak ? Optional.empty() : Optional.of(PLAN);
        }
    }
}
