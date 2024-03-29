package no.nav.foreldrepenger.oversikt.server;

import static org.eclipse.jetty.ee10.webapp.MetaInfConfiguration.CONTAINER_JAR_PATTERN;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.eclipse.jetty.ee10.cdi.CdiDecoratingListener;
import org.eclipse.jetty.ee10.cdi.CdiServletContainerInitializer;
import org.eclipse.jetty.plus.jndi.EnvEntry;
import org.eclipse.jetty.ee10.security.jaspi.DefaultAuthConfigFactory;
import org.eclipse.jetty.ee10.security.jaspi.JaspiAuthenticatorFactory;
import org.eclipse.jetty.ee10.security.jaspi.provider.JaspiAuthConfigProvider;
import org.eclipse.jetty.ee10.servlet.security.ConstraintSecurityHandler;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.jaas.JAASLoginService;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.MDC;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import jakarta.security.auth.message.config.AuthConfigFactory;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.sikkerhet.jaspic.OidcAuthModule;

public class JettyServer {

    private static final Environment ENV = Environment.current();

    private static final String JETTY_SCAN_LOCATIONS = "^.*jersey-.*\\.jar$|^.*felles-.*\\.jar$|^.*app.*\\.jar$";
    private static final String JETTY_LOCAL_CLASSES = "^.*/target/classes/|";
    private final Integer serverPort;

    JettyServer(int serverPort) {
        this.serverPort = serverPort;
    }

    public static void main(String[] args) throws Exception {
        jettyServer().bootStrap();
    }

    private static JettyServer jettyServer() {
        return new JettyServer(ENV.getProperty("server.port", Integer.class, 8080));
    }

    private static ContextHandler createContext() throws MalformedURLException {
        var ctx = new WebAppContext();
        ctx.setParentLoaderPriority(true);

        String descriptor;
        String baseResource;
        try (var factory = ResourceFactory.closeable()) {
            var resource = factory.newClassLoaderResource("/WEB-INF/web.xml", false);
            descriptor = resource.getURI().toURL().toExternalForm();
            baseResource = factory.newResource(".").getRealURI().toURL().toExternalForm();
        }
        ctx.setDescriptor(descriptor);
        ctx.setBaseResourceAsString(baseResource);

        ctx.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
        ctx.setInitParameter("pathInfoOnly", "true");

        // Scanns the CLASSPATH for classes and jars.
        ctx.setAttribute(CONTAINER_JAR_PATTERN, String.format("%s%s", ENV.isLocal() ? JETTY_LOCAL_CLASSES : "", JETTY_SCAN_LOCATIONS));

        // Enable Weld + CDI
        ctx.setInitParameter(CdiServletContainerInitializer.CDI_INTEGRATION_ATTRIBUTE, CdiDecoratingListener.MODE);
        ctx.addServletContainerInitializer(new CdiServletContainerInitializer());
        ctx.addServletContainerInitializer(new org.jboss.weld.environment.servlet.EnhancedListener());

        ctx.setSecurityHandler(createSecurityHandler());
        ctx.setThrowUnavailableOnStartupException(true);

        return ctx;
    }

    private static void konfigurerSikkerhet() {
        if (ENV.isLocal()) {
            initTrustStore();
        }

        var factory = new DefaultAuthConfigFactory();
        factory.registerConfigProvider(new JaspiAuthConfigProvider(new OidcAuthModule()), "HttpServlet", "server /",
            "OIDC Authentication");

        AuthConfigFactory.setFactory(factory);
    }

    private static void initTrustStore() {
        final var trustStorePathProp = "javax.net.ssl.trustStore";
        final var trustStorePasswordProp = "javax.net.ssl.trustStorePassword";

        var defaultLocation = ENV.getProperty("user.home", ".") + "/.modig/truststore.jks";
        var storePath = ENV.getProperty(trustStorePathProp, defaultLocation);
        var storeFile = new File(storePath);
        if (!storeFile.exists()) {
            throw new IllegalStateException(
                "Finner ikke truststore i " + storePath + "\n\tKonfigurer enten som System property '" + trustStorePathProp
                    + "' eller environment variabel '" + trustStorePathProp.toUpperCase().replace('.', '_') + "'");
        }
        var password = ENV.getProperty(trustStorePasswordProp, "changeit");
        System.setProperty(trustStorePathProp, storeFile.getAbsolutePath());
        System.setProperty(trustStorePasswordProp, password);
    }

    private static SecurityHandler createSecurityHandler() {
        var securityHandler = new ConstraintSecurityHandler();
        securityHandler.setAuthenticatorFactory(new JaspiAuthenticatorFactory());
        var loginService = new JAASLoginService();
        loginService.setName("jetty-login");
        loginService.setLoginModuleName("jetty-login");
        loginService.setIdentityService(new DefaultIdentityService());
        securityHandler.setLoginService(loginService);
        return securityHandler;
    }

    void bootStrap() throws Exception {
        System.setProperty("task.manager.runner.threads", "4");
        konfigurerSikkerhet();
        var dataSource = setupDataSource();
        migrer(dataSource);
        start();
    }

    private static void migrer(DataSource dataSource) {
        var flyway = flywayConfig(dataSource);
        flyway.load().migrate();
    }

    public static FluentConfiguration flywayConfig(DataSource dataSource) {
        return Flyway.configure().dataSource(dataSource).locations("classpath:/db/migration/defaultDS").baselineOnMigrate(true);
    }

    public static DataSource setupDataSource() throws NamingException {
        var dataSource = dataSource();
        new EnvEntry("jdbc/defaultDS", dataSource);
        return dataSource;
    }

    public static DataSource dataSource() {
        var config = new HikariConfig();
        config.setJdbcUrl(dbUrl());
        config.setUsername(ENV.getProperty("NAIS_DATABASE_FPOVERSIKT_FPOVERSIKT_USERNAME", "fpoversikt"));
        config.setPassword(ENV.getProperty("NAIS_DATABASE_FPOVERSIKT_FPOVERSIKT_PASSWORD", "fpoversikt"));
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

    private static String dbUrl() {
        var host = ENV.getProperty("NAIS_DATABASE_FPOVERSIKT_FPOVERSIKT_HOST", "fpoversikt");
        var port = ENV.getProperty("NAIS_DATABASE_FPOVERSIKT_FPOVERSIKT_PORT", "5432");
        var databaseName = ENV.getProperty("NAIS_DATABASE_FPOVERSIKT_FPOVERSIKT_DATABASE", "fpoversikt");
        return "jdbc:postgresql://" + host + ":" + port + "/" + databaseName;
    }

    private void start() throws Exception {
        var server = new Server(getServerPort());
        server.setConnectors(createConnectors(server).toArray(new Connector[]{}));
        var handlers = new Handler.Sequence(new ResetLogContextHandler(), createContext());
        server.setHandler(handlers);
        server.start();
        server.join();
    }

    private List<Connector> createConnectors(Server server) {
        List<Connector> connectors = new ArrayList<>();
        var httpConnector = new ServerConnector(server);
        httpConnector.setPort(getServerPort());
        connectors.add(httpConnector);
        return connectors;
    }

    private Integer getServerPort() {
        return this.serverPort;
    }

    /**
     * Legges først slik at alltid resetter context før prosesserer nye requests. Kjøres først så ikke risikerer andre har satt Request#setHandled(true).
     */
    static final class ResetLogContextHandler extends Handler.Abstract {
        @Override
        public boolean handle(Request request, Response response, Callback callback) {
            MDC.clear();
            return false;
        }
    }
}
