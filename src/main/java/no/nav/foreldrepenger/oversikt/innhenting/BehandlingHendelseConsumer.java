package no.nav.foreldrepenger.oversikt.innhenting;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaConsumerManager;
import no.nav.vedtak.server.Controllable;
import no.nav.vedtak.server.LiveAndReadinessAware;

@ApplicationScoped
public class BehandlingHendelseConsumer implements LiveAndReadinessAware, Controllable {

    private static final Logger LOG = LoggerFactory.getLogger(BehandlingHendelseConsumer.class);

    private KafkaConsumerManager<String, String> kcm;

    public BehandlingHendelseConsumer() {
    }

    @Inject
    public BehandlingHendelseConsumer(BehandlingMigreringHåndterer behandlingMigreringHåndterer,
                                      BehandlingHendelseHåndterer behandlingHendelseHåndterer) {
        kcm = new KafkaConsumerManager<>(List.of(behandlingHendelseHåndterer, behandlingMigreringHåndterer));
    }

    @Override
    public boolean isAlive() {
        return kcm.allRunning();
    }

    @Override
    public boolean isReady() {
        return isAlive();
    }

    @Override
    public void start() {
        LOG.info("Starter konsumering av topics={}", kcm.topicNames());
        kcm.start((t, e) -> LOG.error("{} :: Caught exception in stream, exiting", t, e));
    }

    @Override
    public void stop() {
        LOG.info("Starter shutdown av topics={} med 10 sekunder timeout", kcm.topicNames());
        kcm.stop();
    }
}
