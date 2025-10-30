package no.nav.foreldrepenger.oversikt.uttak;

import java.util.Map;

public record KontoBeregningResponseDto(
    Map<String, KontoBeregningDto> result
) {
}
