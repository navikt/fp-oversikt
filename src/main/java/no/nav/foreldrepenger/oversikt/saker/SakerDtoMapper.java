package no.nav.foreldrepenger.oversikt.saker;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.common.innsyn.EsSak;
import no.nav.foreldrepenger.common.innsyn.FpSak;
import no.nav.foreldrepenger.common.innsyn.svp.SvpSak;
import no.nav.foreldrepenger.oversikt.domene.Sak;

final class SakerDtoMapper {

    private SakerDtoMapper() {
    }

    static no.nav.foreldrepenger.common.innsyn.Saker tilDto(List<Sak> saker, PersonOppslagSystem personOppslagSystem) {
        var sakerDtoer = saker.stream()
            .map(s -> map(personOppslagSystem, s))
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

    private static no.nav.foreldrepenger.common.innsyn.Sak map(PersonOppslagSystem personOppslagSystem, Sak s) {
        try {
            return s.tilSakDto(personOppslagSystem);
        } catch (Exception e) {
            throw new IllegalStateException("Feil ved konvertering av sak til sakDto" + s.saksnummer().value(), e);
        }
    }
}
