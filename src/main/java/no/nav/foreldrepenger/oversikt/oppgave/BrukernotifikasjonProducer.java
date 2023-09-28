package no.nav.foreldrepenger.oversikt.oppgave;

import no.nav.brukernotifikasjon.schemas.input.BeskjedInput;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
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
class BrukernotifikasjonProducer {

    private static final Logger LOG = LoggerFactory.getLogger(BrukernotifikasjonProducer.class);

    private static final Environment ENV = Environment.current();

    private String opprettTopic;
    private String avsluttTopic;
    private Producer<NokkelInput, Object> producer;

    @Inject
    BrukernotifikasjonProducer(@KonfigVerdi(value = "dittnav.kafka.topic.opprett") String opprettTopic,
                               @KonfigVerdi(value = "dittnav.kafka.topic.avslutt") String avsluttTopic) {
        this.opprettTopic = opprettTopic;
        this.avsluttTopic = avsluttTopic;
        var properties = KafkaProperties.forProducer();
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        properties.put(AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG, KafkaProperties.getAvroSchemaRegistryBasicAuth());
        properties.put(AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
        properties.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, KafkaProperties.getAvroSchemaRegistryURL());
        this.producer = new KafkaProducer<>(properties);
    }

    BrukernotifikasjonProducer() {
        //CDI
    }

    void opprettBeskjed(BeskjedInput beskjed, NokkelInput nøkkel) {
        send(beskjed, nøkkel, opprettTopic);
    }

    void opprettOppgave(OppgaveInput oppgave, NokkelInput nøkkel) {
        send(oppgave, nøkkel, opprettTopic);
    }

    void sendDone(DoneInput done, NokkelInput nøkkel) {
        send(done, nøkkel, avsluttTopic);
    }

    private void send(Object msg, NokkelInput key, String topic) {
        if (ENV.isLocal() || ENV.isVTP()) {
            LOG.info("Ditt nav kafka er disabled");
            return;
        }

        var melding = new ProducerRecord<>(topic, key, msg);
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
        return new IntegrasjonException("FPOVERSIKT-BRUKERNOTIFIKASJON", "Feil ved publisering av melding på kafka", e);
    }

}
