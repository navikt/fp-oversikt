package no.nav.foreldrepenger.oversikt.server;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RestTest {

    @Test
    void test() {
        assertThat(new Rest().test()).isTrue();
    }
}
