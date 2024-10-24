package no.nav.foreldrepenger.oversikt.innhenting.inntektsmelding;

import jakarta.persistence.EntityManager;
import no.nav.foreldrepenger.common.innsyn.inntektsmelding.FpOversiktInntektsmeldingDto;
import no.nav.foreldrepenger.common.innsyn.inntektsmelding.NaturalytelseType;
import no.nav.foreldrepenger.oversikt.JpaExtension;

import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;
import no.nav.foreldrepenger.oversikt.domene.Beløp;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.inntektsmeldinger.DBInntektsmeldingerRepository;

import no.nav.foreldrepenger.oversikt.innhenting.journalføringshendelse.HentInntektsmeldingerTask;
import no.nav.vedtak.konfig.Tid;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;


@ExtendWith(JpaExtension.class)
public class InntektsmeldingTjenesteTest {

    @Test
    void testInntektsmeldingRoundtrip(EntityManager entityManager) {
        var inntektsmeldingerRepository = new DBInntektsmeldingerRepository(entityManager);

        var bortfalteNaturalytelse1 = new FpSakInntektsmeldingDto.NaturalYtelse(LocalDate.now(), Tid.TIDENES_ENDE, new BigDecimal(1000), NaturalytelseType.BIL);
        var bortfalteNaturalytelse2 = new FpSakInntektsmeldingDto.NaturalYtelse(LocalDate.now(), Tid.TIDENES_ENDE, new BigDecimal(5000), NaturalytelseType.KOST_DØGN);

        var refusjon1 = new FpSakInntektsmeldingDto.Refusjon(new BigDecimal(500), LocalDate.now());

        var fraFpsak = new FpSakInntektsmeldingDto(true, new BigDecimal(50), new BigDecimal(100), new BigDecimal(100), "Arbeidsgiveren", "12312312", "1",
            LocalDateTime.now().minusWeeks(1), LocalDate.now(), List.of(bortfalteNaturalytelse1, bortfalteNaturalytelse2), List.of(refusjon1));
        var im = HentInntektsmeldingerTask.mapV2(fraFpsak);
        var saksnummer = Saksnummer.dummy();
        inntektsmeldingerRepository.lagre(saksnummer, Set.of(im));

        var imTjeneste = new InntektsmeldingTjeneste(inntektsmeldingerRepository);

        var res = imTjeneste.inntektsmeldinger(saksnummer);
        assertThat(res).hasSize(1);
        var hentetIM = res.get(0);
        assertThat(hentetIM.erAktiv()).isEqualTo(fraFpsak.erAktiv());
        assertThat(hentetIM.versjon()).isEqualTo(2);
        assertThat(hentetIM.inntektPrMnd()).isEqualTo(fraFpsak.inntektPrMnd());
        assertThat(hentetIM.refusjonPrMnd()).isEqualTo(fraFpsak.refusjonPrMnd());
        assertThat(hentetIM.arbeidsgiverNavn()).isEqualTo(fraFpsak.arbeidsgiverNavn());
        assertThat(hentetIM.arbeidsgiverIdent()).isEqualTo(fraFpsak.arbeidsgiverIdent());
        assertThat(hentetIM.journalpostId()).isEqualTo(fraFpsak.journalpostId());
        assertThat(hentetIM.mottattTidspunkt()).isEqualTo(fraFpsak.mottattTidspunkt());
        assertThat(hentetIM.startDatoPermisjon()).isEqualTo(fraFpsak.startDatoPermisjon());

        assertThat(hentetIM.bortfalteNaturalytelser()).hasSize(2);
        assertThat(hentetIM.bortfalteNaturalytelser().get(0).fomDato()).isEqualTo(bortfalteNaturalytelse1.fomDato());
        assertThat(hentetIM.bortfalteNaturalytelser().get(0).tomDato()).isEqualTo(bortfalteNaturalytelse1.tomDato());
        assertThat(hentetIM.bortfalteNaturalytelser().get(0).beløpPerMnd()).isEqualTo(bortfalteNaturalytelse1.beløpPerMnd());
        assertThat(hentetIM.bortfalteNaturalytelser().get(0).type()).isEqualTo(NaturalytelseType.BIL);
        assertThat(hentetIM.bortfalteNaturalytelser().get(1).type()).isEqualTo(NaturalytelseType.KOST_DØGN);

        assertThat(hentetIM.refusjonsperioder()).hasSize(1);
        assertThat(hentetIM.refusjonsperioder().get(0).refusjonsbeløpMnd()).isEqualTo(refusjon1.refusjonsbeløpMnd());
        assertThat(hentetIM.refusjonsperioder().get(0).fomDato()).isEqualTo(refusjon1.fomDato());
    }
}
