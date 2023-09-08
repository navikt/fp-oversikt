package no.nav.foreldrepenger.oversikt.innhenting;

import java.util.List;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.innhenting.inntektsmelding.Inntektsmelding;
import no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.DokumentType;

public interface FpsakTjeneste {
    Sak hentSak(Saksnummer saksnummer);

    List<DokumentType> hentMangelendeVedlegg(Saksnummer saksnummer);

    List<Inntektsmelding> hentInntektsmeldinger(Saksnummer saksnummer);
}
