package no.nav.foreldrepenger.oversikt.oppgave;

import java.util.Map;

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

        Serializer<OppgaveInput> result2;
        try (var serde2 = new SpecificAvroSerde<OppgaveInput>(schemaRegistryClient)) {
            result2 = serde2.serializer();
        }
        Serializer<DoneInput> result1;
        try (var serde1 = new SpecificAvroSerde<DoneInput>(schemaRegistryClient)) {
            result1 = serde1.serializer();
        }
        Serializer<NokkelInput> result;
        try (var serde = new SpecificAvroSerde<NokkelInput>(schemaRegistryClient)) {
            result = serde.serializer();
        }

        var properties = KafkaProperties.forProducer();
        this.oppgaveProducer = new KafkaProducer<>(properties, result, result2);
        this.doneProducer = new KafkaProducer<>(properties, result, result1);
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
        if (ENV.isLocal() || ENV.isVTP() || ENV.isProd()) { //TODO prodsett
            LOG.info("Ditt nav kafka er disabled i {}", ENV.clusterName());
            return;
        }

        var melding = new ProducerRecord<>(opprettTopic, key, oppgaveInput);
        try {
            var recordMetadata = oppgaveProducer.send(melding).get();
            LOG.info("Sendte melding til {}, partition {}, offset {}", recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw integrasjonException(e);
        } catch (Exception e) {
            throw integrasjonException(e);
        }
    }

    void sendAvsluttOppgave(DoneInput doneInput, NokkelInput key) {
        if (ENV.isLocal() || ENV.isVTP() || ENV.isProd()) { //TODO prodsett
            LOG.info("Ditt nav kafka er disabled i {}", ENV.clusterName());
            return;
        }

        var melding = new ProducerRecord<>(avsluttTopic, key, doneInput);
        try {
            var recordMetadata = doneProducer.send(melding).get();
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

}
