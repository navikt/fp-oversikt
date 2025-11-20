package no.nav.foreldrepenger.oversikt.innhenting;

import java.util.List;

import no.nav.foreldrepenger.oversikt.arkiv.DokumentTypeHistoriske;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.innhenting.beregning.FpSakBeregningDto;
import no.nav.foreldrepenger.oversikt.innhenting.inntektsmelding.FpSakInntektsmeldingDto;

public interface FpsakTjeneste {
    Sak hentSak(Saksnummer saksnummer);

    List<DokumentTypeHistoriske> hentManglendeVedlegg(Saksnummer saksnummer);

    List<FpSakInntektsmeldingDto> hentInntektsmeldinger(Saksnummer saksnummer);

    List<FpSakBeregningDto> hentBeregninger(Saksnummer saksnummer);

}
