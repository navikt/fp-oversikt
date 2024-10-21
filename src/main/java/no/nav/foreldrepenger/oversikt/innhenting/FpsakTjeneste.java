package no.nav.foreldrepenger.oversikt.innhenting;

import java.util.List;

import no.nav.foreldrepenger.common.domain.felles.DokumentType;
import no.nav.foreldrepenger.common.innsyn.inntektsmelding.FpSakInntektsmeldingDto;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;

public interface FpsakTjeneste {
    Sak hentSak(Saksnummer saksnummer);

    List<DokumentType> hentManglendeVedlegg(Saksnummer saksnummer);

    List<FpSakInntektsmeldingDto> hentInntektsmeldinger(Saksnummer saksnummer);
}
