package no.nav.foreldrepenger.oversikt.oppgave;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;

import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        var properties = KafkaProperties.forProducer();
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        properties.put(KafkaAvroSerializerConfig.USER_INFO_CONFIG, KafkaProperties.getAvroSchemaRegistryBasicAuth());
        properties.put(KafkaAvroSerializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
        properties.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, KafkaProperties.getAvroSchemaRegistryURL());
        this.oppgaveProducer = new KafkaProducer<>(properties);
        this.doneProducer = new KafkaProducer<>(properties);
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

}
