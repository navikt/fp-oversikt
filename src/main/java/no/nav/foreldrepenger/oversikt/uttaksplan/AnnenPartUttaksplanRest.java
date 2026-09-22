package no.nav.foreldrepenger.oversikt.uttaksplan;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import no.nav.foreldrepenger.kontrakter.felles.typer.Saksnummer;
import no.nav.foreldrepenger.kontrakter.fpoversikt.FellesUttaksplanDto;
import no.nav.foreldrepenger.oversikt.uttaksplan.UttaksplanTidslinje.Planperiode;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/uttaksplan/annen-part")
@ApplicationScoped
@Transactional
public class AnnenPartUttaksplanRest {

    private AnnenPartUttaksplanRepository repository;

    @Inject
    public AnnenPartUttaksplanRest(AnnenPartUttaksplanRepository repository) {
        this.repository = repository;
    }

    AnnenPartUttaksplanRest() {
        // CDI
    }

    @POST
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.APPLIKASJON, sporingslogg = true)
    public void lagre(@Valid @NotNull AnnenPartUttaksplanRequest request) {
        var saksnummer = new no.nav.foreldrepenger.oversikt.domene.Saksnummer(request.saksnummer().value());
        var perioder = AnnenPartGraderingFilter.fjernResultatOgArbeidsgivere(request.perioder()
            .stream()
            .map(PeriodeRequest::tilPlanperiode)
            .toList());
        repository.lagre(saksnummer, new AnnenPartUttaksplan(request.mottattTidspunkt(), perioder));
    }

    public record AnnenPartUttaksplanRequest(@Valid @NotNull Saksnummer saksnummer,
                                             @NotNull LocalDateTime mottattTidspunkt,
                                             @NotNull List<@Valid @NotNull PeriodeRequest> perioder) {
    }

    public static final class PeriodeRequest {

        @NotNull
        private final LocalDate fom;
        @NotNull
        private final LocalDate tom;
        @JsonIgnore
        @Valid
        @NotNull
        private final FellesUttaksplanDto.UttakDto uttak;

        @JsonCreator
        public PeriodeRequest(@JsonProperty("fom") LocalDate fom,
                              @JsonProperty("tom") LocalDate tom,
                              @JsonProperty("uttak") FellesUttaksplanDto.UttakDto uttak) {
            this.fom = fom;
            this.tom = tom;
            this.uttak = uttak;
        }

        public LocalDate fom() {
            return fom;
        }

        public LocalDate tom() {
            return tom;
        }

        @JsonProperty("uttak")
        public FellesUttaksplanDto.UttakDto uttak() {
            return uttak;
        }

        @AssertTrue(message = "tom kan ikke være før fom")
        @JsonIgnore
        public boolean isGyldigIntervall() {
            return fom == null || tom == null || !tom.isBefore(fom);
        }

        Planperiode tilPlanperiode() {
            return new Planperiode(fom, tom, uttak);
        }
    }
}
