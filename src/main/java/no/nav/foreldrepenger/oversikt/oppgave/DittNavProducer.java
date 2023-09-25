package no.nav.foreldrepenger.oversikt.oppgave;

import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer;
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
    private Producer<NokkelInput, SpecificRecord> producer;

    @Inject
    DittNavProducer(@KonfigVerdi(value = "dittnav.kafka.topic.opprett") String opprettTopic,
                    @KonfigVerdi(value = "dittnav.kafka.topic.avslutt") String avsluttTopic) {
        this.opprettTopic = opprettTopic;
        this.avsluttTopic = avsluttTopic;
        this.producer = new KafkaProducer<>(KafkaProperties.forProducer(), new SpecificAvroSerializer<>(),
            new SpecificAvroSerializer<>());
    }

    DittNavProducer() {
        //CDI
    }

    void sendOpprettOppgave(OppgaveInput oppgaveInput, NokkelInput key) {
        sendRecord(oppgaveInput, key, opprettTopic);
    }

    void sendAvsluttOppgave(DoneInput doneInput, NokkelInput key) {
        sendRecord(doneInput, key, avsluttTopic);
    }

    private void sendRecord(SpecificRecord record, NokkelInput key, String topic) {
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
