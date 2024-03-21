package no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse;


import static io.confluent.kafka.serializers.KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG;
import static no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.HentDataFraJoarkForHåndteringTask.JOURNALPOST_ID;

import java.util.Map;
import java.util.function.Supplier;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.test.VtpKafkaAvroDeserializer;
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaMessageHandler;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaProperties;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
@ActivateRequestContext
@Transactional
public class JournalføringHendelseHåndterer implements KafkaMessageHandler<String, JournalfoeringHendelseRecord> {

    private static final String GROUP_ID = "fpoversikt-journalføring"; // Hold konstant pga offset commit !!
    private static final String TEMA_FOR = "FOR";
    private static final String MOTTAKSKANAL_ALTINN = "ALTINN";
    private static final String MOTTAKSKANAL_SELVBETJENING = "NAV_NO";
    private static final String HENDELSE_ENDELIG_JOURNALFØRT = "EndeligJournalført";

    private static final Environment ENV = Environment.current();
    private static final Map<String, Object> SCHEMA_MAP = getSchemaMap();

    private String topicName;
    private ProsessTaskTjeneste taskTjeneste;

    JournalføringHendelseHåndterer() {
        // CDI
    }

    @Inject
    public JournalføringHendelseHåndterer(@KonfigVerdi("kafka.topic.journal.hendelse") String topicName,
                                          ProsessTaskTjeneste taskTjeneste) {
        this.topicName = topicName;
        this.taskTjeneste = taskTjeneste;
    }

    @Override
    public void handleRecord(String key, JournalfoeringHendelseRecord value) {
        if (hendelseSkalHåndteres(value)) {
            handleMessage(value);
        }

    }

    private static boolean hendelseSkalHåndteres(JournalfoeringHendelseRecord value) {
        return TEMA_FOR.equals(value.getTemaNytt())
            && (MOTTAKSKANAL_ALTINN.equals(value.getMottaksKanal()) || MOTTAKSKANAL_SELVBETJENING.equals(value.getMottaksKanal()))
            && HENDELSE_ENDELIG_JOURNALFØRT.equals(value.getHendelsesType());
    }

    /*
     * Dokumentasjon https://confluence.adeo.no/display/BOA/Joarkhendelser
     */
    void handleMessage(JournalfoeringHendelseRecord payload) {
        var journalpostId = String.valueOf(payload.getJournalpostId());
        var kanalreferanse = payload.getKanalReferanseId();
        lagreHentFraJoarkTask(journalpostId, kanalreferanse);
    }

    private void lagreHentFraJoarkTask(String journalpostId, String kanalreferanse) {
        var task = ProsessTaskData.forProsessTask(HentDataFraJoarkForHåndteringTask.class);
        if (harVerdi(kanalreferanse)) {
            task.setCallId(kanalreferanse);
        }
        task.setProperty(JOURNALPOST_ID, journalpostId);
        taskTjeneste.lagre(task);
    }

    private static boolean harVerdi(String kanalreferanse) {
        return kanalreferanse != null && !kanalreferanse.isEmpty() && !kanalreferanse.isBlank();
    }

    @Override
    public String topic() {
        return topicName;
    }

    @Override
    public String groupId() {
        return GROUP_ID;
    }

    @Override
    public Supplier<Deserializer<String>> keyDeserializer() {
        return () -> {
            var s = new StringDeserializer();
            s.configure(SCHEMA_MAP, true);
            return s;
        };
    }

    @Override
    public Supplier<Deserializer<JournalfoeringHendelseRecord>> valueDeserializer() {
        return () -> {
            var s = getDeserializer();
            s.configure(SCHEMA_MAP, false);
            return s;
        };
    }

    private static Deserializer<JournalfoeringHendelseRecord> getDeserializer() {
        return ENV.isProd() || ENV.isDev() ? new WrappedAvroDeserializer<>() : new WrappedAvroDeserializer<>(new VtpKafkaAvroDeserializer());
    }

    private static Map<String, Object> getSchemaMap() {
        var schemaRegistryUrl = KafkaProperties.getAvroSchemaRegistryURL();
        if (schemaRegistryUrl != null && !schemaRegistryUrl.isEmpty()) {
            return Map.of(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl,
                AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO",
                AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG, KafkaProperties.getAvroSchemaRegistryBasicAuth(),
                SPECIFIC_AVRO_READER_CONFIG, true);
        } else {
            return Map.of();
        }
    }
}
