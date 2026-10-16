package no.nav.foreldrepenger.oversikt.søknad;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import no.nav.foreldrepenger.kontrakter.felles.typer.Saksnummer;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.TilgangKontrollTjeneste;
import no.nav.foreldrepenger.soknad.kontrakt.foreldrepenger.uttaksplan.FellesUttaksplanDto;

@Path("/søknad")
@ApplicationScoped
@Transactional
public class SøknadRest {

    private SøknadRepository repository;
    private TilgangKontrollTjeneste tilgangKontroll;

    @Inject
    public SøknadRest(SøknadRepository repository, TilgangKontrollTjeneste tilgangKontroll) {
        this.repository = repository;
        this.tilgangKontroll = tilgangKontroll;
    }

    SøknadRest() {
        // CDI
    }

    @POST
    public void lagre(@Valid @NotNull SøknadRequest request) {
        tilgangKontroll.sjekkAtKallErFraSystem();
        var saksnummer = new no.nav.foreldrepenger.oversikt.domene.Saksnummer(request.saksnummer().value());
        var perioder = request.perioder().stream().map(SøknadMapper::tilPeriode).toList();
        repository.lagre(saksnummer, new Søknad(request.mottattTidspunkt().toInstant(), request.rolle(), perioder));
    }

    public record SøknadRequest(@Valid @NotNull Saksnummer saksnummer,
                               @NotNull OffsetDateTime mottattTidspunkt,
                               @Valid @NotNull Søknad.Rolle rolle,
                               @NotNull List<@Valid @NotNull PeriodeRequest> perioder) {
        @Override
        public String toString() {
            return "SøknadRequest[skjermet]";
        }
    }

    public record PeriodeRequest(@NotNull LocalDate fom,
                                 @NotNull LocalDate tom,
                                 @Valid @NotNull FellesUttaksplanDto.UttakDto uttak) {
        @AssertTrue(message = "tom kan ikke være før fom")
        @JsonIgnore
        public boolean isGyldigIntervall() {
            return fom == null || tom == null || !tom.isBefore(fom);
        }

        @Override
        public String toString() {
            return "PeriodeRequest[skjermet]";
        }
    }
}
