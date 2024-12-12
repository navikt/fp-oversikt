package no.nav.foreldrepenger.oversikt;

import static no.nav.foreldrepenger.oversikt.server.JettyServer.flywayConfig;

import java.util.Properties;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.eclipse.jetty.plus.jndi.EnvEntry;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.extension.TestInstantiationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareExtension;

public class JpaExtension extends EntityManagerAwareExtension {

    private static DataSource DS;

    private static final String TEST_DB_CONTAINER = Environment.current().getProperty("testcontainer.test.db", String.class, "postgres:17-alpine");
    private static final PostgreSQLContainer TEST_DATABASE;

    static {
        TEST_DATABASE = new PostgreSQLContainer<>(DockerImageName.parse(TEST_DB_CONTAINER))
            .withReuse(true);
        TEST_DATABASE.start();

        var dataSource = datasSource(TEST_DATABASE.getJdbcUrl(), TEST_DATABASE.getUsername(), TEST_DATABASE.getPassword());
        var flyway = flywayConfig(dataSource).cleanDisabled(false).load();
        try {
            flyway.migrate();
        } catch (FlywayException e) {
            flyway.clean();
            flyway.migrate();
        }
    }

    private synchronized static DataSource datasSource(String jdbcUrl, String username, String password) {
        if (DS == null) {
            try {
                DS = setupDataSource(jdbcUrl, username, password);
                new EnvEntry("jdbc/defaultDS", DS);
            } catch (NamingException e) {
                throw new TestInstantiationException("Problemer med å sette oppe datasource", e);
            }
        }
        return DS;
    }

    private static DataSource setupDataSource(String jdbcUrl, String username, String password) {
        var config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMinimumIdle(1);
        config.setMaximumPoolSize(6);
        config.setIdleTimeout(10001);
        config.setMaxLifetime(30001);
        config.setInitializationFailTimeout(30000);
        config.setConnectionTestQuery("select 1");
        config.setDriverClassName("org.postgresql.Driver");
        config.setAutoCommit(false);

        // optimaliserer inserts for postgres
        var dsProperties = new Properties();
        dsProperties.setProperty("reWriteBatchedInserts", "true");
        dsProperties.setProperty("logServerErrorDetail", "false"); // skrur av batch exceptions som lekker statements i åpen logg
        config.setDataSourceProperties(dsProperties);

        return new HikariDataSource(config);
    }
}
