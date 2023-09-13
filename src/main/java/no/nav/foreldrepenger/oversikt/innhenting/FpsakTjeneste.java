package no.nav.foreldrepenger.oversikt.innhenting;

import java.util.List;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.innhenting.inntektsmelding.Inntektsmelding;
import no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.DokumentTypeId;

public interface FpsakTjeneste {
    Sak hentSak(Saksnummer saksnummer);

    List<DokumentTypeId> hentMangelendeVedlegg(Saksnummer saksnummer);

    List<Inntektsmelding> hentInntektsmeldinger(Saksnummer saksnummer);
}
