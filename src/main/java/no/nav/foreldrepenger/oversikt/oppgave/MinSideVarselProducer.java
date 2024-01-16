package no.nav.foreldrepenger.oversikt.oppgave;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaProperties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@ApplicationScoped
class MinSideVarselProducer {

    private static final Logger LOG = LoggerFactory.getLogger(MinSideVarselProducer.class);

    private static final Environment ENV = Environment.current();
    private String topic;
    private Producer<String, String> producer;

    @Inject
    MinSideVarselProducer(@KonfigVerdi(value = "minside.kafka.varseltopic") String varselTopic) {
        this.topic = varselTopic;
        this.producer = new KafkaProducer<>(KafkaProperties.forProducer());
    }

    MinSideVarselProducer() {
        //CDI
    }

    void send(UUID key, String json) {
        send(topic, key, json);
    }

    private void send(String topic, UUID key, String msg) {
        if (ENV.isLocal() || ENV.isVTP()) {
            LOG.info("Dummy MinSideVarsel-producer sender msg {}", msg);
            return;
        }

        var melding = new ProducerRecord<>(topic, key.toString(), msg);
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
        return new IntegrasjonException("FPOVERSIKT-MINSIDEVARSEL", "Feil ved publisering av melding på kafka", e);
    }

}
