package no.nav.foreldrepenger.oversikt.server;

import org.slf4j.bridge.SLF4JBridgeHandler;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.oversikt.server.konfig.ApiConfig;
import no.nav.foreldrepenger.oversikt.server.konfig.ForvaltningApiConfig;
import no.nav.foreldrepenger.oversikt.server.konfig.InternalApiConfig;
import no.nav.vedtak.felles.jpa.NamingStandard;
import no.nav.vedtak.felles.jpa.flyway.FlywayUtil;
import no.nav.vedtak.felles.jpa.jdbc.DataSourceHolder;
import no.nav.vedtak.felles.jpa.jdbc.DatasourceUtil;
import no.nav.vedtak.log.metrics.MetricsUtil;
import no.nav.vedtak.server.jetty.DataSourceShutdownListener;
import no.nav.vedtak.server.jetty.JettyServerBuilder;

public class JettyServer {

    private static final Environment ENV = Environment.current();

    private static final String CONTEXT_PATH = ENV.getProperty("context.path", "/fpoversikt");

    private final Integer serverPort;

    JettyServer(int serverPort) {
        this.serverPort = serverPort;
    }

    static void main() throws Exception {
        jettyServer().bootStrap();
    }

    private static JettyServer jettyServer() {
        return new JettyServer(ENV.getProperty("server.port", Integer.class, 8080));
    }

    void bootStrap() throws Exception {
        MetricsUtil.init();
        System.setProperty("task.manager.runner.threads", "4");
        konfigurerLogging();
        var ds = DatasourceUtil.postgresDataSource(ENV.getRequiredProperty("DB_JDBC_URL"), null, null, 16);
        DataSourceHolder.initialize(ds);
        FlywayUtil.migrate(ds, NamingStandard.DEFAULT_DS_MIGRATION_CLASSPATH);
        start();
    }

    /**
     * Vi bruker SLF4J + logback, Jersey brukes JUL for logging.
     * Setter opp en bridge til å få Jersey til å logge gjennom Logback også.
     */
    private void konfigurerLogging() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    private void start() throws Exception {
        var server = JettyServerBuilder.builder()
            .port(getServerPort())
            .contextPath(CONTEXT_PATH)
            .addEventListener(new ServiceStarterListener())
            .addEventListener(new DataSourceShutdownListener(DataSourceHolder::close))
            .registerRestApp(InternalApiConfig.API_URI, InternalApiConfig.class)
            .registerRestApp(ApiConfig.API_URI, ApiConfig.class)
            .registerRestApp(ForvaltningApiConfig.API_URI, ForvaltningApiConfig.class)
            .build();
        server.start();
        server.join();
    }

    private Integer getServerPort() {
        return this.serverPort;
    }

}
