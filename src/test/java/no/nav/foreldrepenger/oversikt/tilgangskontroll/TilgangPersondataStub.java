package no.nav.foreldrepenger.oversikt.tilgangskontroll;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.pdlpip.TilgangPersondata;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.pdlpip.TilgangPersondataDto;

public record TilgangPersondataStub(TilgangPersondataDto tilgangPersondataDto) implements TilgangPersondata {

    public static TilgangPersondataStub tilgangpersondata(AktørId aktørId) {
        return tilgangpersondata(aktørId, LocalDate.now().minusYears(20));
    }

    public static TilgangPersondataStub tilgangpersondata(AktørId aktørId, LocalDate fødselsdato) {
        var person = new TilgangPersondataDto.Person(
            null,
            List.of(new TilgangPersondataDto.Fødsel(fødselsdato)),
            null,
            null
        );
        var tilgangPersondataDto = new TilgangPersondataDto(aktørId.value(), person, null, null);
        return new TilgangPersondataStub(tilgangPersondataDto);
    }

    @Override
    public TilgangPersondataDto hentTilgangPersondata(String ident) {
        return tilgangPersondataDto;
    }

    @Override
    public Map<String, TilgangPersondataDto> hentTilgangPersondataBolk(List<String> identer) {
        var persondata = new HashMap<String, TilgangPersondataDto>();
        persondata.put(identer.getFirst(), tilgangPersondataDto);
        return persondata;
    }
}
