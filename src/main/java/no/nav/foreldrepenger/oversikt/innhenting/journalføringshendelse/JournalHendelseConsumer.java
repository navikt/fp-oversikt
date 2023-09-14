package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;

import static org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_CLIENT;

import java.time.Duration;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaProperties;
import no.nav.vedtak.log.metrics.Controllable;
import no.nav.vedtak.log.metrics.LiveAndReadinessAware;

/*
 * Dokumentasjon https://confluence.adeo.no/display/BOA/Joarkhendelser
 */
@ApplicationScoped
public class JournalHendelseConsumer implements LiveAndReadinessAware, Controllable {

    private static final Logger LOG = LoggerFactory.getLogger(JournalHendelseConsumer.class);

    private static final String APPLICATION_ID = "fpoversikt-journalføring"; // Hold konstant pga offset commit !!

    private static final String TEMA_FOR = "FOR";
    private static final String MOTTAKSKANAL_ALTINN = "ALTINN";
    private static final String MOTTAKSKANAL_SELVBETJENING = "NAV_NO";
    private static final String HENDELSE_ENDELIG_JOURNALFØRT = "EndeligJournalført";

    private KafkaStreams stream;
    private Topic<String, JournalfoeringHendelseRecord> topic;

    JournalHendelseConsumer() {
        // CDI
    }

    @Inject
    public JournalHendelseConsumer(@KonfigVerdi("kafka.topic.journal.hendelse") String topicName,
                                   JournalføringHendelseHåndterer journalføringHendelseHåndterer) {
        this.topic = Topic.createConfiguredTopic(topicName);
        this.stream = createKafkaStreams(topic, journalføringHendelseHåndterer);
    }

    private static KafkaStreams createKafkaStreams(Topic<String, JournalfoeringHendelseRecord> topic,
                                                   JournalføringHendelseHåndterer journalføringHendelseHåndterer) {
        final Consumed<String, JournalfoeringHendelseRecord> consumed = Consumed.<String, JournalfoeringHendelseRecord>with(
            Topology.AutoOffsetReset.LATEST).withKeySerde(topic.serdeKey()).withValueSerde(topic.serdeValue());

        final StreamsBuilder builder = new StreamsBuilder();
        builder.stream(topic.topic(), consumed)
            .filter((key, value) -> TEMA_FOR.equals(value.getTemaNytt()))
            .peek((key, value) -> LOG.info("Mottok hendelse {} med id {} fra kanal {} journalpostId", value.getHendelsesType(), value.getKanalReferanseId(), value.getMottaksKanal(), value.getJournalpostId()))
            .filter((key, value) -> MOTTAKSKANAL_ALTINN.equals(value.getMottaksKanal()) || MOTTAKSKANAL_SELVBETJENING.equals(value.getMottaksKanal()))
            .filter((key, value) -> HENDELSE_ENDELIG_JOURNALFØRT.equals(value.getHendelsesType()))
            .foreach((key, value) -> journalføringHendelseHåndterer.handleMessage(value));

        return new KafkaStreams(builder.build(), KafkaProperties.forStreamsGenericValue(APPLICATION_ID, topic.serdeValue()));
    }

    private void addShutdownHooks() {
        stream.setStateListener((newState, oldState) -> {
            LOG.info("{} :: From state={} to state={}", getTopicName(), oldState, newState);

            if (newState == KafkaStreams.State.ERROR) {
                // if the stream has died there is no reason to keep spinning
                LOG.warn("{} :: No reason to keep living, closing stream", getTopicName());
                stop();
            }
        });
        stream.setUncaughtExceptionHandler(ex -> {
            LOG.error("{} :: Caught exception in stream, exiting", getTopicName(), ex);
            return SHUTDOWN_CLIENT;
        });
    }

    @Override
    public void start() {
        addShutdownHooks();

        stream.start();
        LOG.info("Starter konsumering av topic={}, tilstand={}", getTopicName(), stream.state());
    }

    public String getTopicName() {
        return topic.topic();
    }

    @Override
    public boolean isAlive() {
        return (stream != null) && stream.state().isRunningOrRebalancing();
    }

    @Override
    public boolean isReady() {
        return isAlive();
    }

    @Override
    public void stop() {
        LOG.info("Starter shutdown av topic={}, tilstand={} med 15 sekunder timeout", getTopicName(), stream.state());
        stream.close(Duration.ofSeconds(15));
        LOG.info("Shutdown av topic={}, tilstand={} med 15 sekunder timeout", getTopicName(), stream.state());
    }
}
