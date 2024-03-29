package no.nav.foreldrepenger.oversikt;

import static no.nav.foreldrepenger.oversikt.server.JettyServer.flywayConfig;
import static no.nav.foreldrepenger.oversikt.server.JettyServer.setupDataSource;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.junit.jupiter.api.extension.TestInstantiationException;

import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareExtension;

public class JpaExtension extends EntityManagerAwareExtension {

    private static DataSource DS;

    static {
        var dataSource = datasSource();
        var flyway = flywayConfig(dataSource).cleanDisabled(false).cleanOnValidationError(true).load();
        flyway.migrate();
    }

    private synchronized static DataSource datasSource() {
        if (DS == null) {
            try {
                DS = setupDataSource();
            } catch (NamingException e) {
                throw new TestInstantiationException("Problemer med å sette oppe datasource", e);
            }
        }
        return DS;
    }
}
