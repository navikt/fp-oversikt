package no.nav.foreldrepenger.oversikt.server;

import static no.nav.foreldrepenger.oversikt.server.JettyServer.dataSource;
import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class HealtCheckRestServiceTest {

    @Test
    void test() {
        assertThat(new HealtCheckRest().isAlive().getStatus()).isEqualTo(200);

        var dataSource = dataSource();

        var flyway = Flyway.configure().dataSource(dataSource).baselineOnMigrate(true);

        flyway.load().migrate();
    }
}
