package no.nav.foreldrepenger.oversikt.integrasjoner.ereg;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.vedtak.felles.integrasjon.organisasjon.OrgInfo;
import no.nav.vedtak.util.LRUCache;

@ApplicationScoped
public class VirksomhetTjeneste {

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(24, TimeUnit.HOURS);

    private static final String KUNSTIG_ORG = "342352362";  // magic constant
    private static final String KUNSTIG_NAVN = "Kunstig virksomhet";

    private static final LRUCache<String, String> CACHE = new LRUCache<>(3000, CACHE_ELEMENT_LIVE_TIME_MS);

    private OrgInfo eregRestKlient;

    public VirksomhetTjeneste() {
        // CDI
    }

    @Inject
    public VirksomhetTjeneste(OrgInfo eregRestKlient) {
        this.eregRestKlient = eregRestKlient;
    }

    public static boolean erOrganisasjonsNummer(String orgNummer) {
        return orgNummer != null && orgNummer.matches("\\d{9}");
    }

    public String hentOrganisasjonNavn(String orgNummer) {
        if (Objects.equals(KUNSTIG_ORG, orgNummer)) {
            return KUNSTIG_NAVN;
        }
        var virksomhetNavn = Optional.ofNullable(CACHE.get(orgNummer))
            .or(() -> Optional.ofNullable(hentOrganisasjonRest(orgNummer)))
            .orElse("Ukjent virksomhet");
        CACHE.put(orgNummer, virksomhetNavn);
        return virksomhetNavn;
    }

    private String hentOrganisasjonRest(String orgNummer) {
        try {
            return eregRestKlient.hentOrganisasjonNavn(orgNummer);
        } catch (Exception e) {
            return null;
        }

    }

}
