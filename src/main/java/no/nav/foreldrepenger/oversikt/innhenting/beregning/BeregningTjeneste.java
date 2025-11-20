package no.nav.foreldrepenger.oversikt.innhenting.beregning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.beregning.Beregning;
import no.nav.foreldrepenger.oversikt.domene.beregning.BeregningRepository;

import java.util.Collections;
import java.util.Set;

@ApplicationScoped
public class BeregningTjeneste {
    private BeregningRepository beregningRepository;

    @Inject
    public BeregningTjeneste(BeregningRepository beregningRepository) {
        this.beregningRepository = beregningRepository;
    }

    BeregningTjeneste() {
        // CDI
    }

    public Set<Beregning> finnBeregning(@Valid @NotNull Saksnummer saksnummer) {
        // TODO: usikker på hvorfor set
        return beregningRepository.hentFor(Collections.singleton(saksnummer));
    }
}
