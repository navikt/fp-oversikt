package no.nav.foreldrepenger.oversikt.oppgave;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Sak;
import no.nav.foreldrepenger.oversikt.domene.SakRepository;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.tilbakekreving.Tilbakekreving;
import no.nav.foreldrepenger.oversikt.domene.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.oversikt.domene.vedlegg.manglende.ManglendeVedleggRepository;
import no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.DokumentType;

@ApplicationScoped
public class Oppgaver {

    private ManglendeVedleggRepository manglendeVedleggRepository;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private SakRepository sakRepository;

    @Inject
    public Oppgaver(ManglendeVedleggRepository manglendeVedleggRepository,
                    TilbakekrevingRepository tilbakekrevingRepository,
                    SakRepository sakRepository) {
        this.manglendeVedleggRepository = manglendeVedleggRepository;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.sakRepository = sakRepository;
    }

    public Oppgaver() {
    }

    List<DokumentType> manglendeVedlegg(Saksnummer saksnummer) {
        return manglendeVedleggRepository.hentFor(saksnummer);
    }

    Set<TilbakekrevingsInnslag> tilbakekrevingsuttalelser(AktørId aktørId) {
        var saker = sakRepository.hentFor(aktørId).stream()
            .map(Sak::saksnummer)
            .collect(Collectors.toSet());

        return tilbakekrevingRepository.hentFor(saker).stream()
            .filter(Tilbakekreving::varsleBrukerOmTilbakekreving)
            .map(Oppgaver::tilDto)
            .collect(Collectors.toSet());
    }

    private static TilbakekrevingsInnslag tilDto(Tilbakekreving tilbakekreving) {
        return new TilbakekrevingsInnslag(tilbakekreving.saksnummer());
    }
}
