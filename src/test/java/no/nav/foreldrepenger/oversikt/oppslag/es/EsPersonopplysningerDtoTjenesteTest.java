package no.nav.foreldrepenger.oversikt.oppslag.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.oversikt.integrasjoner.pdl.PdlKlient;
import no.nav.foreldrepenger.oversikt.oppslag.felles.Kjønn;
import no.nav.foreldrepenger.oversikt.stub.DummyInnloggetTestbruker;
import no.nav.pdl.Foedselsdato;
import no.nav.pdl.Folkeregisteridentifikator;
import no.nav.pdl.Kjoenn;
import no.nav.pdl.KjoennType;
import no.nav.pdl.Navn;
import no.nav.pdl.Person;

@ExtendWith(MockitoExtension.class)
class EsPersonopplysningerDtoTjenesteTest {

    @Mock
    private PdlKlient pdlKlient;

    private EsPersonopplysningerDtoTjeneste tjeneste(DummyInnloggetTestbruker innloggetBruker) {
        return new EsPersonopplysningerDtoTjeneste(pdlKlient, innloggetBruker);
    }

    @Test
    void søker_happy_case() {
        var innloggetBruker = DummyInnloggetTestbruker.myndigInnloggetBruker();
        var tjeneste = tjeneste(innloggetBruker);

        var søkerPdl = lagPerson("KARI", "MELLOM", "KANARI", KjoennType.KVINNE, LocalDate.now().minusYears(28));
        søkerPdl.setFolkeregisteridentifikator(List.of(new Folkeregisteridentifikator(innloggetBruker.fødselsnummer().value(), "I_BRUK", "FNR", null, null)));

        when(pdlKlient.hentPerson(any(), any())).thenReturn(søkerPdl);

        var dto = tjeneste.forInnloggetPerson();

        assertThat(dto).isNotNull();
        assertThat(dto.fnr()).isEqualTo(innloggetBruker.fødselsnummer());
        assertThat(dto.navn().fornavn()).isEqualTo("Kari");
        assertThat(dto.navn().mellomnavn()).isEqualTo("Mellom");
        assertThat(dto.navn().etternavn()).isEqualTo("Kanari");
        assertThat(dto.kjønn()).isEqualTo(Kjønn.K);
        assertThat(dto.fødselsdato()).isEqualTo(LocalDate.now().minusYears(28));
    }

    private static Person lagPerson(String fornavn, String mellomnavn, String etternavn, KjoennType kjønn, LocalDate fødselsdato) {
        var person = new Person();
        person.setNavn(List.of(new Navn(fornavn, mellomnavn, etternavn, null, null, null, null, null)));
        person.setKjoenn(List.of(new Kjoenn(kjønn, null, null)));
        person.setFoedselsdato(List.of(new Foedselsdato(fødselsdato.format(DateTimeFormatter.ISO_LOCAL_DATE), null, null, null)));
        return person;
    }
}

