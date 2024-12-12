package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaConsumerManager;
import no.nav.vedtak.server.Controllable;
import no.nav.vedtak.server.LiveAndReadinessAware;

@ApplicationScoped
public class JournalHendelseConsumer implements LiveAndReadinessAware, Controllable {

    private static final Logger LOG = LoggerFactory.getLogger(JournalHendelseConsumer.class);

    private KafkaConsumerManager<String, JournalfoeringHendelseRecord> kcm;

    JournalHendelseConsumer() {
        // CDI
    }

    @Inject
    public JournalHendelseConsumer(JournalføringHendelseHåndterer journalføringHendelseHåndterer) {
        this.kcm = new KafkaConsumerManager<>(journalføringHendelseHåndterer);
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
