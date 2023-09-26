package no.nav.foreldrepenger.oversikt.stub;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.innhenting.FpsakTjeneste;
import no.nav.foreldrepenger.oversikt.innhenting.Sak;
import no.nav.foreldrepenger.oversikt.innhenting.inntektsmelding.Inntektsmelding;
import no.nav.foreldrepenger.oversikt.arkiv.DokumentTypeId;

public class FpsakTjenesteStub implements FpsakTjeneste {

    private final Map<Saksnummer, Sak> saker = new ConcurrentHashMap<>();
    private final Map<Saksnummer, List<DokumentTypeId>> manglendeVedlegg = new ConcurrentHashMap<>();
    private final Map<Saksnummer, List<Inntektsmelding>> inntektsmeldinger = new ConcurrentHashMap<>();

    public FpsakTjenesteStub leggTilSak(Sak sak) {
        saker.put(new Saksnummer(sak.saksnummer()), sak);
        return this;
    }

    public FpsakTjenesteStub leggTilManglendeVedlegg(Saksnummer saksnummer, List<DokumentTypeId> vedlegg) {
        manglendeVedlegg.put(saksnummer, List.copyOf(vedlegg));
        return this;
    }

    public FpsakTjenesteStub leggTilIMs(Saksnummer saksnummer, List<Inntektsmelding> ims) {
        inntektsmeldinger.put(saksnummer, List.copyOf(ims));
        return this;
    }

    @Override
    public Sak hentSak(Saksnummer saksnummer) {
        return saker.get(saksnummer);
    }

    @Override
    public List<DokumentTypeId> hentMangelendeVedlegg(Saksnummer saksnummer) {
        return manglendeVedlegg.get(saksnummer);
    }

    @Override
    public List<Inntektsmelding> hentInntektsmeldinger(Saksnummer saksnummer) {
        return inntektsmeldinger.get(saksnummer);
    }
}
