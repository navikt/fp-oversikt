package no.nav.foreldrepenger.oversikt.server;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HealtCheckRestServiceTest {

    @Test
    void test() {
        assertThat(new HealtCheckRest().isAlive().getStatus()).isEqualTo(200);
    }
}
