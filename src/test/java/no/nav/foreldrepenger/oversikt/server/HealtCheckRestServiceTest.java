package no.nav.foreldrepenger.oversikt.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

class HealtCheckRestServiceTest {

    @Test
    void test() {
        assertThat(new HealtCheckRest().isAlive().getStatus()).isEqualTo(200);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:hsqldb:mem:testdb;sql.syntax_pgs=true");
        config.setUsername("sa");
        config.setPassword("");
        HikariDataSource dataSource = new HikariDataSource(config);


        var flyway = Flyway.configure().dataSource(dataSource).baselineOnMigrate(true);

        flyway.load().migrate();
    }
}
