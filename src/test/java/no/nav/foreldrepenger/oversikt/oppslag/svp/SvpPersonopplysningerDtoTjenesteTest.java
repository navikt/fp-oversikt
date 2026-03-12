package no.nav.foreldrepenger.oversikt.oppslag.svp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.oversikt.arbeid.EksternArbeidsforholdDto;
import no.nav.foreldrepenger.oversikt.arbeid.Stillingsprosent;
import no.nav.foreldrepenger.oversikt.integrasjoner.pdl.PdlKlient;
import no.nav.foreldrepenger.oversikt.oppslag.felles.Kjønn;
import no.nav.foreldrepenger.oversikt.oppslag.felles.MineArbeidsforholdTjeneste;
import no.nav.foreldrepenger.oversikt.stub.DummyInnloggetTestbruker;
import no.nav.pdl.Foedselsdato;
import no.nav.pdl.Folkeregisteridentifikator;
import no.nav.pdl.Kjoenn;
import no.nav.pdl.KjoennType;
import no.nav.pdl.Navn;
import no.nav.pdl.Person;

@ExtendWith(MockitoExtension.class)
class SvpPersonopplysningerDtoTjenesteTest {

    @Mock
    private PdlKlient pdlKlient;

    @Mock
    private MineArbeidsforholdTjeneste mineArbeidsforholdTjeneste;

    private SvpPersonopplysningerDtoTjeneste tjeneste(DummyInnloggetTestbruker innloggetBruker) {
        return new SvpPersonopplysningerDtoTjeneste(pdlKlient, mineArbeidsforholdTjeneste, innloggetBruker);
    }

    @Test
    void søker_happy_case_med_arbeidsforhold() {
        var innloggetBruker = DummyInnloggetTestbruker.myndigInnloggetBruker();
        var tjeneste = tjeneste(innloggetBruker);

        var søkerPdl = lagPerson("KARI", null, "KANARI", KjoennType.KVINNE, LocalDate.now().minusYears(28));
        søkerPdl.setFolkeregisteridentifikator(List.of(new Folkeregisteridentifikator(innloggetBruker.fødselsnummer().value(), "I_BRUK", "FNR", null, null)));

        var arbeidsforhold = List.of(
            new EksternArbeidsforholdDto("123456789", "org", "Arbeidsgiver AS", Stillingsprosent.arbeid(BigDecimal.valueOf(100)),
                LocalDate.now().minusYears(5), null)
        );

        when(pdlKlient.hentPerson(any(), any())).thenReturn(søkerPdl);
        when(mineArbeidsforholdTjeneste.brukersArbeidsforhold(any())).thenReturn(arbeidsforhold);

        var dto = tjeneste.forInnloggetPerson();

        assertThat(dto).isNotNull();
        assertThat(dto.fnr()).isEqualTo(innloggetBruker.fødselsnummer());
        assertThat(dto.navn().fornavn()).isEqualTo("Kari");
        assertThat(dto.navn().etternavn()).isEqualTo("Kanari");
        assertThat(dto.kjønn()).isEqualTo(Kjønn.K);
        assertThat(dto.fødselsdato()).isEqualTo(LocalDate.now().minusYears(28));
        assertThat(dto.arbeidsforhold()).hasSize(1);
        assertThat(dto.arbeidsforhold().getFirst().arbeidsgiverNavn()).isEqualTo("Arbeidsgiver AS");
        assertThat(dto.arbeidsforhold().getFirst().stillingsprosent().prosent()).isEqualTo(BigDecimal.valueOf(100));
        assertThat(dto.arbeidsforhold().getFirst().fom()).isEqualTo(LocalDate.now().minusYears(5));
        assertThat(dto.arbeidsforhold().getFirst().tom()).isNull();
    }

    private static Person lagPerson(String fornavn, String mellomnavn, String etternavn, KjoennType kjønn, LocalDate fødselsdato) {
        var person = new Person();
        person.setNavn(List.of(new Navn(fornavn, mellomnavn, etternavn, null, null, null, null, null)));
        person.setKjoenn(List.of(new Kjoenn(kjønn, null, null)));
        person.setFoedselsdato(List.of(new Foedselsdato(fødselsdato.format(DateTimeFormatter.ISO_LOCAL_DATE), null, null, null)));
        return person;
    }
}

