package no.nav.foreldrepenger.oversikt.uttaksplan;

import static no.nav.foreldrepenger.oversikt.KontekstTestHelper.innloggetBorger;
import static no.nav.foreldrepenger.oversikt.KontekstTestHelper.innloggetSaksbehandlerUtenDrift;
import static no.nav.foreldrepenger.oversikt.stub.DummyInnloggetTestbruker.myndigInnloggetBruker;
import static no.nav.foreldrepenger.oversikt.stub.DummyInnloggetTestbruker.umyndigInnloggetBruker;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

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

    private final RegistrerendeTjeneste tjeneste = new RegistrerendeTjeneste();

    @Test
    void uautentisertKallSkalIkkeKunneHenteUttaksplan() {
        KontekstHolder.fjernKontekst();
        var personOppslag = PersonOppslagStub.ugradert();

        assertThatThrownBy(() -> rest(myndigInnloggetBruker(), personOppslag).hent(requestMedAnnenPart()))
            .isExactlyInstanceOf(OversiktManglerTilgangException.class);
        assertThat(tjeneste.kall).isEmpty();
        assertThat(personOppslag.antallOppslag).isZero();
    }

    @Test
    void ansattSkalIkkeKunneHenteUttaksplan() {
        innloggetSaksbehandlerUtenDrift();
        var personOppslag = PersonOppslagStub.ugradert();

        assertThatThrownBy(() -> rest(myndigInnloggetBruker(), personOppslag).hent(requestMedAnnenPart()))
            .isExactlyInstanceOf(OversiktManglerTilgangException.class);
        assertThat(tjeneste.kall).isEmpty();
        assertThat(personOppslag.antallOppslag).isZero();
    }

    @Test
    void umyndigBorgerSkalIkkeKunneHenteUttaksplan() {
        innloggetBorger();
        var personOppslag = PersonOppslagStub.ugradert();

        assertThatThrownBy(() -> rest(umyndigInnloggetBruker(), personOppslag).hent(requestMedAnnenPart()))
            .isInstanceOf(OversiktManglerTilgangException.class)
            .extracting(e -> ((OversiktManglerTilgangException) e).getFeilkode())
            .isEqualTo(LokalFeilKode.IKKE_TILGANG_UMYNDIG.name());
        assertThat(tjeneste.kall).isEmpty();
        assertThat(personOppslag.antallOppslag).isZero();
    }

    @Test
    void ugradertAnnenPartSendesVerifisertTilTjenestenMedInnloggetBrukerSomSøker() {
        innloggetBorger();
        var søker = AktørId.dummy();

        var plan = rest(myndigInnloggetBruker(søker), PersonOppslagStub.ugradert()).hent(requestMedAnnenPart());

        assertThat(plan).isSameAs(RegistrerendeTjeneste.PLAN);
        assertThat(tjeneste.kall).containsExactly(new Kall(søker, new VerifisertAnnenPart(ANNEN_PART)));
    }

    private enum SkjultAnnenPart { BESKYTTET, UKJENT_IDENT, UKJENT_I_PDL, MANGLENDE_ADRESSEBESKYTTELSE, IKKE_OPPGITT }

    @ParameterizedTest
    @EnumSource(SkjultAnnenPart.class)
    void skjultAnnenPartSendesSomIkkeOppgittTilTjenesten(SkjultAnnenPart tilfelle) {
        innloggetBorger();
        var søker = AktørId.dummy();
        var personOppslag = switch (tilfelle) {
            case BESKYTTET -> PersonOppslagStub.medAdresse(new AdresseBeskyttelse(Set.of(AdresseBeskyttelse.Gradering.GRADERT)));
            case UKJENT_IDENT -> PersonOppslagStub.medIdentfeil(new NoSuchElementException());
            case UKJENT_I_PDL -> PersonOppslagStub.medAdressefeil(new BrukerIkkeFunnetIPdlException());
            case MANGLENDE_ADRESSEBESKYTTELSE -> PersonOppslagStub.medAdresse(null);
            case IKKE_OPPGITT -> PersonOppslagStub.ugradert();
        };
        var request = tilfelle == SkjultAnnenPart.IKKE_OPPGITT ? requestUtenAnnenPart() : requestMedAnnenPart();

        var plan = rest(myndigInnloggetBruker(søker), personOppslag).hent(request);

        assertThat(plan).isSameAs(RegistrerendeTjeneste.PLAN);
        assertThat(tjeneste.kall).containsExactly(new Kall(søker, null));
    }

    @Test
    void tekniskPdlFeilPropagererUtenTjenestekall() {
        innloggetBorger();
        var feil = new IllegalStateException("PDL utilgjengelig");

        assertThatThrownBy(() -> rest(myndigInnloggetBruker(), PersonOppslagStub.medIdentfeil(feil)).hent(requestMedAnnenPart()))
            .isSameAs(feil);
        assertThatThrownBy(() -> rest(myndigInnloggetBruker(), PersonOppslagStub.medAdressefeil(feil)).hent(requestMedAnnenPart()))
            .isSameAs(feil);
        assertThat(tjeneste.kall).isEmpty();
    }

    private UttaksplanRest rest(InnloggetBruker innloggetBruker, PersonOppslagSystem personOppslag) {
        return new UttaksplanRest(tjeneste, new TilgangKontrollTjeneste(null, innloggetBruker), innloggetBruker, personOppslag);
    }

    private static UttaksplanRest.FellesUttaksplanRequest requestMedAnnenPart() {
        return new UttaksplanRest.FellesUttaksplanRequest(new UttaksplanRest.BarnIdentifikator(null, FAMILIEHENDELSE), ANNEN_PART_FNR);
    }

    private static UttaksplanRest.FellesUttaksplanRequest requestUtenAnnenPart() {
        return new UttaksplanRest.FellesUttaksplanRequest(new UttaksplanRest.BarnIdentifikator(null, FAMILIEHENDELSE), null);
    }

    private record Kall(AktørId søker, VerifisertAnnenPart annenPart) {
    }

    private static class RegistrerendeTjeneste extends UttaksplanTjeneste {
        static final FellesUttaksplanDto PLAN = new FellesUttaksplanDto(FAMILIEHENDELSE, 1, FellesUttaksplanDto.Dekningsgrad.HUNDRE, List.of());
        final List<Kall> kall = new ArrayList<>();

        @Override
        public Optional<FellesUttaksplanDto> hentFor(AktørId søker, VerifisertAnnenPart annenPart, AktørId barn, LocalDate familiehendelse) {
            kall.add(new Kall(søker, annenPart));
            return Optional.of(PLAN);
        }
    }

    private static final class PersonOppslagStub implements PersonOppslagSystem {
        private final RuntimeException identfeil;
        private final Exception adressefeil;
        private final AdresseBeskyttelse adresse;
        int antallOppslag;

        private PersonOppslagStub(RuntimeException identfeil, Exception adressefeil, AdresseBeskyttelse adresse) {
            this.identfeil = identfeil;
            this.adressefeil = adressefeil;
            this.adresse = adresse;
        }

        static PersonOppslagStub ugradert() {
            return medAdresse(new AdresseBeskyttelse(Set.of(AdresseBeskyttelse.Gradering.UGRADERT)));
        }

        static PersonOppslagStub medAdresse(AdresseBeskyttelse adresse) {
            return new PersonOppslagStub(null, null, adresse);
        }

        static PersonOppslagStub medIdentfeil(RuntimeException feil) {
            return new PersonOppslagStub(feil, null, null);
        }

        static PersonOppslagStub medAdressefeil(Exception feil) {
            return new PersonOppslagStub(null, feil, null);
        }

        @Override
        public AktørId aktørId(Fødselsnummer fnr) {
            antallOppslag++;
            if (identfeil != null) {
                throw identfeil;
            }
            return ANNEN_PART_FNR.equals(fnr) ? ANNEN_PART : new AktørId(fnr.value());
        }

        @Override
        public Fødselsnummer fødselsnummer(AktørId aktørId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AdresseBeskyttelse adresseBeskyttelse(Fødselsnummer fnr) throws BrukerIkkeFunnetIPdlException {
            antallOppslag++;
            if (adressefeil instanceof BrukerIkkeFunnetIPdlException e) {
                throw e;
            }
            if (adressefeil instanceof RuntimeException e) {
                throw e;
            }
            return adresse;
        }

        @Override
        public String navn(String ident) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean barnHarDisseForeldrene(Fødselsnummer barn, Fødselsnummer mor, Fødselsnummer annenForelder) {
            throw new UnsupportedOperationException();
        }
    }
}
