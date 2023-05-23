package no.nav.foreldrepenger.oversikt.saker;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.common.innsyn.EsSak;
import no.nav.foreldrepenger.common.innsyn.FpSak;
import no.nav.foreldrepenger.common.innsyn.SvpSak;
import no.nav.foreldrepenger.oversikt.domene.Sak;

final class SakerDtoMapper {

    private SakerDtoMapper() {
    }

    static no.nav.foreldrepenger.common.innsyn.Saker tilDto(List<Sak> saker, FødselsnummerOppslag fnrOppslag) {
        var sakerDtoer = saker.stream()
            .filter(Sak::harSakSøknad)
            .map(s -> s.tilSakDto(fnrOppslag))
            .collect(Collectors.toSet());

        var foreldrepenger = sakerDtoer.stream()
            .filter(FpSak.class::isInstance)
            .map(FpSak.class::cast)
            .collect(Collectors.toSet());
        var svangeskapspenger = sakerDtoer.stream()
            .filter(SvpSak.class::isInstance)
            .map(SvpSak.class::cast)
            .collect(Collectors.toSet());
        var engangsstønad = sakerDtoer.stream()
            .filter(EsSak.class::isInstance)
            .map(EsSak.class::cast)
            .collect(Collectors.toSet());
        return new no.nav.foreldrepenger.common.innsyn.Saker(foreldrepenger, engangsstønad, svangeskapspenger);
    }
}
