package no.nav.foreldrepenger.oversikt.oppgave;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.SøknadStatus;
import no.nav.foreldrepenger.oversikt.domene.fp.BrukerRolle;
import no.nav.foreldrepenger.oversikt.domene.fp.Dekningsgrad;
import no.nav.foreldrepenger.oversikt.domene.fp.FpSøknad;
import no.nav.foreldrepenger.oversikt.domene.fp.FpSøknadsperiode;
import no.nav.foreldrepenger.oversikt.domene.fp.Konto;
import no.nav.foreldrepenger.oversikt.domene.fp.MorsAktivitet;
import no.nav.foreldrepenger.oversikt.domene.fp.SakFP0;
import no.nav.foreldrepenger.oversikt.stub.RepositoryStub;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

class MinSideBeskjedMorsArbeidTaskTest {

    @Test
    void sender_beskjed() {
        // Arrange
        var sakRepository = new RepositoryStub();
        var minSideTjeneste = mock(MinSideTjeneste.class);

        var morsAktørId = AktørId.dummy();
        var sak = lagFarSak(Konto.FELLESPERIODE, MorsAktivitet.ARBEID, morsAktørId, true);
        sakRepository.lagre(sak);

        var task = new MinSideBeskjedMorsArbeidTask(minSideTjeneste, sakRepository);
        var data = ProsessTaskData.forTaskType(TaskType.forProsessTask(MinSideBeskjedMorsArbeidTask.class));
        data.setSaksnummer(sak.saksnummer().value());
        task.doTask(data);

        verify(minSideTjeneste).sendBeskjedMorsArbeid(eq(morsAktørId), any(UUID.class));
    }

    @Test
    void sender_ikke_beskjed_hvis_ingen_annen_part() {
        // Arrange
        var sakRepository = new RepositoryStub();
        var minSideTjeneste = mock(MinSideTjeneste.class);

        var sak = lagFarSak(Konto.FELLESPERIODE, MorsAktivitet.ARBEID, null, true);
        sakRepository.lagre(sak);

        var task = new MinSideBeskjedMorsArbeidTask(minSideTjeneste, sakRepository);
        var data = ProsessTaskData.forTaskType(TaskType.forProsessTask(MinSideBeskjedMorsArbeidTask.class));
        data.setSaksnummer(sak.saksnummer().value());
        task.doTask(data);

        verify(minSideTjeneste, times(0)).sendBeskjedMorsArbeid(any(), any(UUID.class));
    }

    @Test
    void sender_ikke_beskjed_hvis_ikke_morArbeidUtenDok() {
        // Arrange
        var sakRepository = new RepositoryStub();
        var minSideTjeneste = mock(MinSideTjeneste.class);

        var morsAktørId = AktørId.dummy();
        var sak = lagFarSak(Konto.FELLESPERIODE, MorsAktivitet.ARBEID, morsAktørId, false);
        sakRepository.lagre(sak);

        var task = new MinSideBeskjedMorsArbeidTask(minSideTjeneste, sakRepository);
        var data = ProsessTaskData.forTaskType(TaskType.forProsessTask(MinSideBeskjedMorsArbeidTask.class));
        data.setSaksnummer(sak.saksnummer().value());
        task.doTask(data);

        verify(minSideTjeneste, times(0)).sendBeskjedMorsArbeid(any(), any(UUID.class));
    }

    private static SakFP0 lagFarSak(Konto kontoType, MorsAktivitet arbeid, AktørId morsAktørId, boolean morArbeidUtenDok) {
        var søknad = lagSøknad(kontoType, arbeid, morArbeidUtenDok);
        return new SakFP0(Saksnummer.dummy(), AktørId.dummy(), false, Set.of(), morsAktørId, null, Set.of(), Set.of(søknad), BrukerRolle.FAR,
            Set.of(), null, false, null);
    }

    private static FpSøknad lagSøknad(Konto kontoType, MorsAktivitet arbeid, boolean morArbeidUtenDok) {
        var søknadsperiode = new FpSøknadsperiode(LocalDate.now(), LocalDate.now(), kontoType, null, null, null, null, null, null, arbeid);
        return new FpSøknad(SøknadStatus.MOTTATT, LocalDateTime.now(), Set.of(søknadsperiode), Dekningsgrad.ÅTTI, morArbeidUtenDok);
    }
}
