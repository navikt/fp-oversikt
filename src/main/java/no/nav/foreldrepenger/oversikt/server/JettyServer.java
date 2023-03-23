package no.nav.foreldrepenger.oversikt.server;

import static org.eclipse.jetty.webapp.MetaInfConfiguration.CONTAINER_JAR_PATTERN;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.message.config.AuthConfigFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.jaas.JAASLoginService;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.jaspi.DefaultAuthConfigFactory;
import org.eclipse.jetty.security.jaspi.JaspiAuthenticatorFactory;
import org.eclipse.jetty.security.jaspi.provider.JaspiAuthConfigProvider;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.MDC;

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
        jettyServer(args).bootStrap();
    }

    private static JettyServer jettyServer(String[] args) {
        return new JettyServer(ENV.getProperty("server.port", Integer.class, 8080));
    }

    private static ContextHandler createContext() throws MalformedURLException {
        var ctx = new WebAppContext();
        ctx.setParentLoaderPriority(true);

        ctx.setResourceBase(".");
        ctx.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
        ctx.setInitParameter("pathInfoOnly", "true");

        // Scanns the CLASSPATH for classes and jars.
        ctx.setAttribute(CONTAINER_JAR_PATTERN, String.format("%s%s", ENV.isLocal() ? JETTY_LOCAL_CLASSES : "", JETTY_SCAN_LOCATIONS));

        // WELD init
        ctx.addEventListener(new org.jboss.weld.environment.servlet.Listener());
        ctx.addEventListener(new org.jboss.weld.environment.servlet.BeanManagerResourceBindingListener());

        String descriptor;
        try (var resource = Resource.newClassPathResource("/WEB-INF/web.xml")) {
            descriptor = resource.getURI().toURL().toExternalForm();
        }
        ctx.setDescriptor(descriptor);

        ctx.setSecurityHandler(createSecurityHandler());
        ctx.setThrowUnavailableOnStartupException(true);

        return ctx;
    }

    private static void konfigurerSikkerhet() {
        var factory = new DefaultAuthConfigFactory();
        factory.registerConfigProvider(new JaspiAuthConfigProvider(new OidcAuthModule()), "HttpServlet", "server /",
            "OIDC Authentication");

        AuthConfigFactory.setFactory(factory);
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
        konfigurerSikkerhet();
        start();
    }

    private void start() throws Exception {
        var server = new Server(getServerPort());
        server.setConnectors(createConnectors(server).toArray(new Connector[]{}));
        var handlers = new HandlerList(new ResetLogContextHandler(), createContext());
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
    static final class ResetLogContextHandler extends AbstractHandler {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
            MDC.clear();
        }
    }
}
