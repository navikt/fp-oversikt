package no.nav.foreldrepenger.oversikt.oppgave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.net.MalformedURLException;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.YtelseType;
import no.nav.foreldrepenger.oversikt.stub.DummyPersonOppslagSystemTest;
import no.nav.tms.varsel.builder.BuilderEnvironment;


class MinSideTjenesteTest {

    private MinSideProducer producer;
    private MinSideTjeneste tjeneste;

    @BeforeEach
    void setup() throws MalformedURLException {
        var pdl = new DummyPersonOppslagSystemTest(null, null);
        producer = mock(MinSideProducer.class);
        tjeneste = new MinSideTjeneste(pdl, null, producer, "https://www.nav.no/foreldrepenger/oversikt", "https://www.nav.no/foreldrepenger");
        BuilderEnvironment.extend(Map.of(
            "NAIS_CLUSTER_NAME", "test",
            "NAIS_NAMESPACE", "test",
            "NAIS_APP_NAME", "test"
        ));
    }

    @Test
    void skal_sende_beskjed_om_mottatt_søknad() {
        var uuidArgument = ArgumentCaptor.forClass(UUID.class);
        var jsonArgument = ArgumentCaptor.forClass(String.class);
        var aktørId = new AktørId("12345678901");
        tjeneste.sendBeskjedVedInnkommetSøknad(aktørId, YtelseType.FORELDREPENGER, true, UUID.randomUUID());

        verify(producer).send(uuidArgument.capture(), jsonArgument.capture());

        assertThat(jsonArgument.getValue()).contains("Vi mottok en søknad om endring av foreldrepenger");
    }

}
