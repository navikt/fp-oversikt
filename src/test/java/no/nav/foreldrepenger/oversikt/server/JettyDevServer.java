package no.nav.foreldrepenger.oversikt.server;

import no.nav.vedtak.server.localdev.LocalDevProperties;

public class JettyDevServer extends JettyServer {

    public static void main(String[] args) throws Exception {
        // Konfigurerer tasker til å polle mer aggressivt, gjør at verdikjede kjører raskere lokalt
        System.setProperty("task.manager.polling.delay", "40");
        System.setProperty("task.manager.runner.threads", "4");
        LocalDevProperties.setPropertiesForLocalDev();
        jettyServer(args).bootStrap();
    }

    protected static JettyDevServer jettyServer(String[] args) {
        // Resolve paths
        if (args.length > 0) {
            return new JettyDevServer(Integer.parseUnsignedInt(args[0]));
        }
        return new JettyDevServer(8889);
    }

    private JettyDevServer(int serverPort) {
        super(serverPort);
    }

}
