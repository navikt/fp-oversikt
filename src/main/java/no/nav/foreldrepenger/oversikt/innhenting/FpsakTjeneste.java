package no.nav.foreldrepenger.oversikt.innhenting;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import no.nav.foreldrepenger.common.innsyn.Dekningsgrad;

public interface FpsakTjeneste {
    SakDto hentSak(UUID behandlingUuid);

    record SakDto(String saksnummer, String aktørId, Set<VedtakDto> vedtakene) {

        public record VedtakDto(LocalDateTime vedtakstidspunkt, UttakDto uttak) {
        }

        public record UttakDto(Dekningsgrad dekningsgrad, List<UttaksperiodeDto> perioder) {
        }

        public record UttaksperiodeDto(LocalDate fom, LocalDate tom) {
        }
    }
}
