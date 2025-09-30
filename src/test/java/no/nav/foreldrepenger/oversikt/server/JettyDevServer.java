package no.nav.foreldrepenger.oversikt.server;

import no.nav.foreldrepenger.konfig.Environment;

public class JettyDevServer extends JettyServer {

    private static final Environment ENV = Environment.current();

    public static void main(String[] args) throws Exception {
        // Konfigurerer tasker til å polle mer aggressivt, gjør at verdikjede kjører raskere lokalt
        System.setProperty("task.manager.polling.delay", "40");
        System.setProperty("task.manager.runner.threads", "4");
        initTrustStoreAndKeyStore();
        jettyServer(args).bootStrap();
    }

    protected static JettyDevServer jettyServer(String[] args) {
        // Resolve paths
        String absolutePathHome = System.getProperty("user.home");
        String keystoreRelativPath = ENV.getProperty("keystore.relativ.path");
        String truststoreRelativPath = ENV.getProperty("truststore.relativ.path");
        String keystoreTruststorePassword = ENV.getProperty("vtp.ssl.passord");

        System.out.println("user.home: " + absolutePathHome);
        System.out.println("keystore.relativ.path: " + keystoreRelativPath);
        System.out.println("truststore.relativ.path: " + truststoreRelativPath);
        System.out.println("vtp.ssl.passord: " + keystoreTruststorePassword);
        System.out.println("TrustStore path: " + absolutePathHome + truststoreRelativPath + " exists: " + new java.io.File(absolutePathHome + truststoreRelativPath).exists());
        System.out.println("KeyStore path: " + absolutePathHome + keystoreRelativPath + " exists: " + new java.io.File(absolutePathHome + keystoreRelativPath).exists());


        if (args.length > 0) {
            return new JettyDevServer(Integer.parseUnsignedInt(args[0]));
        }
        return new JettyDevServer(ENV.getProperty("server.port", Integer.class, 8080));
    }

    private JettyDevServer(int serverPort) {
        super(serverPort);
    }

    private static void initTrustStoreAndKeyStore() {
        var keystoreRelativPath = ENV.getProperty("keystore.relativ.path");
        var truststoreRelativPath = ENV.getProperty("truststore.relativ.path");
        var keystoreTruststorePassword = ENV.getProperty("vtp.ssl.passord");
        var absolutePathHome = ENV.getProperty("user.home", ".");
        System.setProperty("javax.net.ssl.trustStore", absolutePathHome + truststoreRelativPath);
        System.setProperty("javax.net.ssl.keyStore", absolutePathHome + keystoreRelativPath);
        System.setProperty("javax.net.ssl.trustStorePassword", keystoreTruststorePassword);
        System.setProperty("javax.net.ssl.keyStorePassword", keystoreTruststorePassword);
        System.setProperty("javax.net.ssl.password", keystoreTruststorePassword);
        // KAFKA spesifikke properties
        System.setProperty("KAFKA_TRUSTSTORE_PATH", absolutePathHome + truststoreRelativPath);
        System.setProperty("KAFKA_KEYSTORE_PATH", absolutePathHome + keystoreRelativPath);
        System.setProperty("KAFKA_CREDSTORE_PASSWORD", keystoreTruststorePassword);
    }

}
