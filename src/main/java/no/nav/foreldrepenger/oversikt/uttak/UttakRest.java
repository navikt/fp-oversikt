package no.nav.foreldrepenger.oversikt.uttak;

import static no.nav.foreldrepenger.oversikt.uttak.KontoBeregningDtoMapper.tilKontoberegning;
import static no.nav.foreldrepenger.stønadskonto.regelmodell.grunnlag.Dekningsgrad.DEKNINGSGRAD_100;
import static no.nav.foreldrepenger.stønadskonto.regelmodell.grunnlag.Dekningsgrad.DEKNINGSGRAD_80;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.nav.foreldrepenger.stønadskonto.regelmodell.StønadskontoRegelOrkestrering;
import no.nav.foreldrepenger.stønadskonto.regelmodell.grunnlag.BeregnKontoerGrunnlag;
import no.nav.foreldrepenger.stønadskonto.regelmodell.grunnlag.Dekningsgrad;

@Path("/konto")
@ApplicationScoped
public class UttakRest {

    private static final StønadskontoRegelOrkestrering REGEL_ORKESTRERING = new StønadskontoRegelOrkestrering();



    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, KontoBeregningDto> personinfo(@Valid @NotNull KontoBeregningGrunnlagDto grunnlag) {
        guardFamiliehendelse(grunnlag);
        var kontoberegning80 = kontoberegningFra(grunnlag, DEKNINGSGRAD_80);
        var kontoberegning100 = kontoberegningFra(grunnlag, DEKNINGSGRAD_100);
        return Map.of(
            "80", kontoberegning80,
            "100", kontoberegning100
        );
    }

    private KontoBeregningDto kontoberegningFra(KontoBeregningGrunnlagDto grunnlag, Dekningsgrad dekningsgrad) {
        var stønadskontoer = REGEL_ORKESTRERING.beregnKontoer(tilBeregnKontoGrunnlag(grunnlag, dekningsgrad)).getStønadskontoer();
        return tilKontoberegning(stønadskontoer, grunnlag);
    }

    private BeregnKontoerGrunnlag tilBeregnKontoGrunnlag(KontoBeregningGrunnlagDto grunnlag, Dekningsgrad dekningsgrad) {
        return new BeregnKontoerGrunnlag.Builder()
            .dekningsgrad(dekningsgrad)
            .rettighetType(grunnlag.rettighetstype())
            .brukerRolle(grunnlag.brukerrolle())
            .antallBarn(grunnlag.antallBarn())
            .fødselsdato(grunnlag.fødselsdato())
            .termindato(grunnlag.termindato())
            .omsorgsovertakelseDato(grunnlag.omsorgsovertakelseDato())
            .morHarUføretrygd(grunnlag.morHarUføretrygd())
            .familieHendelseDatoNesteSak(grunnlag.familieHendelseDatoNesteSak())
            .build();
    }

    private static void guardFamiliehendelse(KontoBeregningGrunnlagDto grunnlag) {
        if (grunnlag.fødselsdato() == null && grunnlag.termindato() == null && grunnlag.omsorgsovertakelseDato() == null) {
            throw new IllegalStateException("Mangler dato for familiehendelse");
        }
    }
}
