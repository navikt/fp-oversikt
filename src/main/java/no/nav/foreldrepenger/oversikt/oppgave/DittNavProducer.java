package no.nav.foreldrepenger.oversikt.oppgave;

import java.util.Map;

import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaProperties;

@ApplicationScoped
class DittNavProducer {

    private static final Logger LOG = LoggerFactory.getLogger(DittNavProducer.class);

    private static final Environment ENV = Environment.current();

    private String opprettTopic;
    private String avsluttTopic;
    private Producer<NokkelInput, OppgaveInput> oppgaveProducer;
    private Producer<NokkelInput, DoneInput> doneProducer;

    @Inject
    DittNavProducer(@KonfigVerdi(value = "dittnav.kafka.topic.opprett") String opprettTopic,
                    @KonfigVerdi(value = "dittnav.kafka.topic.avslutt") String avsluttTopic) {
        this.opprettTopic = opprettTopic;
        this.avsluttTopic = avsluttTopic;

        var schemaRegistryClient = schemaRegistryClient();

        Serializer<OppgaveInput> oppgaveInputSerializer = serializer(schemaRegistryClient);
        Serializer<DoneInput> doneInputSerializer = serializer(schemaRegistryClient);
        Serializer<NokkelInput> nokkelInputSerializer = serializer(schemaRegistryClient);

        var properties = KafkaProperties.forProducer();
        this.oppgaveProducer = new KafkaProducer<>(properties, nokkelInputSerializer, oppgaveInputSerializer);
        this.doneProducer = new KafkaProducer<>(properties, nokkelInputSerializer, doneInputSerializer);
    }

    private static CachedSchemaRegistryClient schemaRegistryClient() {
        var schemaRegistryClientConfig = Map.of(
            AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO",
            AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG, KafkaProperties.getAvroSchemaRegistryBasicAuth()
        );
        return new CachedSchemaRegistryClient(KafkaProperties.getAvroSchemaRegistryURL(), 10, schemaRegistryClientConfig);
    }

    DittNavProducer() {
        //CDI
    }

    void sendOpprettOppgave(OppgaveInput oppgaveInput, NokkelInput key) {
        sendRecord(oppgaveInput, key, opprettTopic, oppgaveProducer);
    }

    void sendAvsluttOppgave(DoneInput doneInput, NokkelInput key) {
        sendRecord(doneInput, key, avsluttTopic, doneProducer);
    }

    private static <T extends SpecificRecord> void sendRecord(T record, NokkelInput key, String topic, Producer<NokkelInput, T> producer) {
        if (ENV.isLocal() || ENV.isVTP() || ENV.isProd()) { //TODO prodsett
            LOG.info("Ditt nav kafka er disabled i {}", ENV.clusterName());
            return;
        }

        var melding = new ProducerRecord<>(topic, key, record);
        try {
            var recordMetadata = producer.send(melding).get();
            LOG.info("Sendte melding til {}, partition {}, offset {}", recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw integrasjonException(e);
        } catch (Exception e) {
            throw integrasjonException(e);
        }
    }

    private static IntegrasjonException integrasjonException(Exception e) {
        return new IntegrasjonException("FPOVERSIKT-DITTNAV", "Feil ved publisering av melding på kafka", e);
    }

    private static <T extends SpecificRecord> Serializer<T> serializer(CachedSchemaRegistryClient schemaRegistryClient) {
        try (var serde = new SpecificAvroSerde<T>(schemaRegistryClient)) {
            return serde.serializer();
        }
    }
}
