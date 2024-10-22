package no.nav.foreldrepenger.oversikt.domene.svp;

import static java.time.LocalDate.now;
import static no.nav.foreldrepenger.oversikt.domene.svp.Aktivitet.Type;
import static no.nav.foreldrepenger.oversikt.domene.svp.OppholdPeriode.Årsak.FERIE;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.common.innsyn.BehandlingTilstand;
import no.nav.foreldrepenger.common.innsyn.svp.Vedtak;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.domene.Arbeidsgiver;
import no.nav.foreldrepenger.oversikt.domene.FamilieHendelse;
import no.nav.foreldrepenger.oversikt.domene.Prosent;
import no.nav.foreldrepenger.oversikt.domene.Saksnummer;
import no.nav.foreldrepenger.oversikt.domene.SøknadStatus;
import no.nav.foreldrepenger.oversikt.stub.TilgangKontrollStub;

class DtoMapperTest {

    @Test
    void mapper_til_dto() {
        var termindato = now().plusWeeks(4);
        var antallBarn = 1;
        var aktivitet = new Aktivitet(Type.ORDINÆRT_ARBEID, Arbeidsgiver.dummy(), null);
        var arbeidstidprosent = new Prosent(50);
        var oppholdPeriode = new OppholdPeriode(now(), now(), FERIE, OppholdPeriode.OppholdKilde.SAKSBEHANDLER);
        var tlPeriode = new TilretteleggingPeriode(now(), TilretteleggingType.DELVIS, arbeidstidprosent);
        var tilrettelegging = new Tilrettelegging(aktivitet, now(), "risiko", "tiltak",
            Set.of(tlPeriode),
            Set.of());
        var svpPeriode = new SvpPeriode(now().minusWeeks(1), now().plusWeeks(1), TilretteleggingType.DELVIS, arbeidstidprosent, arbeidstidprosent,
            ResultatÅrsak.INNVILGET);
        var arbeidsforholdVedtak = new ArbeidsforholdUttak(aktivitet, now(), "ris", "til",
            Set.of(svpPeriode),
            Set.of(oppholdPeriode), null);
        var vedtak = new SvpVedtak(LocalDateTime.now(), Set.of(arbeidsforholdVedtak), SvpVedtak.AvslagÅrsak.MANGLENDE_DOKUMENTASJON);
        var sakSVP0 = new SakSVP0(Saksnummer.dummy(), AktørId.dummy(), false, new FamilieHendelse(null, termindato, antallBarn, null),
            Set.of(), Set.of(new SvpSøknad(SøknadStatus.MOTTATT, LocalDateTime.now(), Set.of(tilrettelegging))), Set.of(vedtak),
            LocalDateTime.now());

        var dto = sakSVP0.tilSakDto(TilgangKontrollStub.borger(true));
        assertThat(dto.sakAvsluttet()).isFalse();
        assertThat(dto.familiehendelse().antallBarn()).isEqualTo(antallBarn);
        assertThat(dto.familiehendelse().fødselsdato()).isNull();
        assertThat(dto.familiehendelse().termindato()).isEqualTo(termindato);
        assertThat(dto.familiehendelse().omsorgsovertakelse()).isNull();

        assertThat(dto.åpenBehandling().tilstand()).isEqualTo(BehandlingTilstand.UNDER_BEHANDLING);

        assertThat(dto.gjeldendeVedtak().avslagÅrsak()).isEqualTo(Vedtak.AvslagÅrsak.MANGLENDE_DOKUMENTASJON);
        assertThat(dto.gjeldendeVedtak().arbeidsforhold()).hasSize(1);
        var arbeidsforhold = dto.gjeldendeVedtak().arbeidsforhold().stream().findFirst().get();
        assertThat(arbeidsforhold.tilrettelegginger()).hasSize(2);
        //Splittet på opphold
        var sortert = arbeidsforhold.tilrettelegginger()
            .stream()
            .sorted(Comparator.comparing(no.nav.foreldrepenger.common.innsyn.svp.Tilrettelegging::fom))
            .toList();
        var tl1 = sortert.get(0);
        var tl2 = sortert.get(1);
        assertThat(tl1.fom()).isEqualTo(svpPeriode.fom());
        assertThat(tl1.tom()).isEqualTo(oppholdPeriode.fom().minusDays(1));
        assertThat(tl1.arbeidstidprosent().value()).isEqualTo(arbeidstidprosent.decimalValue());
        assertThat(tl1.type()).isEqualTo(no.nav.foreldrepenger.common.innsyn.svp.TilretteleggingType.DELVIS);
        assertThat(tl2.fom()).isEqualTo(oppholdPeriode.tom().plusDays(1));
        assertThat(tl2.tom()).isEqualTo(svpPeriode.tom());
        assertThat(tl2.arbeidstidprosent().value()).isEqualTo(arbeidstidprosent.decimalValue());
        assertThat(tl2.type()).isEqualTo(no.nav.foreldrepenger.common.innsyn.svp.TilretteleggingType.DELVIS);
        assertThat(arbeidsforhold.oppholdsperioder()).hasSize(1);
        assertThat(arbeidsforhold.oppholdsperioder().stream().toList().get(0).oppholdKilde()).isEqualTo(no.nav.foreldrepenger.common.innsyn.svp.OppholdPeriode.OppholdKilde.SAKSBEHANDLER);

        assertThat(dto.åpenBehandling().søknad().arbeidsforhold()).hasSize(1);
        var arbeidsforholdSøknad = dto.åpenBehandling().søknad().arbeidsforhold().stream().findFirst().orElseThrow();
        assertThat(arbeidsforholdSøknad.aktivitet().arbeidsgiver().id()).isEqualTo(aktivitet.arbeidsgiver().identifikator());
        assertThat(arbeidsforholdSøknad.aktivitet().type()).isEqualTo(no.nav.foreldrepenger.common.innsyn.Aktivitet.Type.ORDINÆRT_ARBEID);
        assertThat(arbeidsforholdSøknad.behovFrom()).isEqualTo(tilrettelegging.behovFom());
        assertThat(arbeidsforholdSøknad.tiltak()).isEqualTo(tilrettelegging.tiltak());
        assertThat(arbeidsforholdSøknad.risikofaktorer()).isEqualTo(tilrettelegging.risikoFaktorer());
        assertThat(arbeidsforholdSøknad.oppholdsperioder()).isEmpty();

        var tilretteleggingSøknad = arbeidsforholdSøknad.tilrettelegginger().stream().findFirst().orElseThrow();
        assertThat(tilretteleggingSøknad.fom()).isEqualTo(tlPeriode.fom());
        assertThat(tilretteleggingSøknad.tom()).isEqualTo(termindato.minusWeeks(3).minusDays(1));
    }

}
