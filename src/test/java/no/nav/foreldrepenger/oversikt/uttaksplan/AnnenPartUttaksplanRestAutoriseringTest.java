package no.nav.foreldrepenger.oversikt.uttaksplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.konfig.Namespace;
import no.nav.foreldrepenger.oversikt.tilgangskontroll.PdpRequestBuilderImpl;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacAuditlogger;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.PepImpl;
import no.nav.vedtak.sikkerhet.abac.internal.BeskyttetRessursAttributter;
import no.nav.vedtak.sikkerhet.oidc.config.AzureProperty;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.tilgang.AnsattGruppeKlient;
import no.nav.vedtak.sikkerhet.tilgang.PopulasjonKlient;

@ExtendWith(MockitoExtension.class)
class AnnenPartUttaksplanRestAutoriseringTest {

    private static final String FPSOKNAD = "vtp:" + Namespace.foreldrepenger().getName() + ":fpsoknad";
    private static final String EKSTERN_APPLIKASJON = "vtp:annetnamespace:ekstern-applikasjon";

    @Mock
    private AbacAuditlogger auditlogger;
    @Mock
    private PopulasjonKlient populasjonKlient;
    @Mock
    private AnsattGruppeKlient ansattGruppeKlient;

    @BeforeAll
    static void setUpEnvironment() {
        System.setProperty(AzureProperty.AZURE_APP_PRE_AUTHORIZED_APPS.name(), FPSOKNAD + "," + EKSTERN_APPLIKASJON);
    }

    @AfterAll
    static void tearDownEnvironment() {
        System.clearProperty(AzureProperty.AZURE_APP_PRE_AUTHORIZED_APPS.name());
    }

    @Test
    void skalGiTilgangTilPreautorisertSystemressurs() throws NoSuchMethodException {
        var pep = new PepImpl(auditlogger, populasjonKlient, ansattGruppeKlient, new PdpRequestBuilderImpl());
        var attributter = attributter(IdentType.Systemressurs, FPSOKNAD);

        assertThat(pep.vurderTilgang(attributter).fikkTilgang()).isTrue();
        verifyNoInteractions(populasjonKlient, ansattGruppeKlient);
    }

    @Test
    void skalAvviseIkkePreautorisertSystemressurs() throws NoSuchMethodException {
        var pep = new PepImpl(auditlogger, populasjonKlient, ansattGruppeKlient, new PdpRequestBuilderImpl());
        var attributter = attributter(IdentType.Systemressurs, "vtp:teamforeldrepenger:ukjent-applikasjon");

        assertThat(pep.vurderTilgang(attributter).fikkTilgang()).isFalse();
        verifyNoInteractions(populasjonKlient, ansattGruppeKlient);
    }

    @Test
    void skalAvvisePreautorisertSystemressursFraAnnetNamespace() throws NoSuchMethodException {
        var pep = new PepImpl(auditlogger, populasjonKlient, ansattGruppeKlient, new PdpRequestBuilderImpl());
        var attributter = attributter(IdentType.Systemressurs, EKSTERN_APPLIKASJON);

        assertThat(pep.vurderTilgang(attributter).fikkTilgang()).isFalse();
        verifyNoInteractions(populasjonKlient, ansattGruppeKlient);
    }

    @Test
    void skalAvviseEksternBruker() throws NoSuchMethodException {
        var pep = new PepImpl(auditlogger, populasjonKlient, ansattGruppeKlient, new PdpRequestBuilderImpl());
        var attributter = attributter(IdentType.EksternBruker, "12345678901");

        assertThat(pep.vurderTilgang(attributter).fikkTilgang()).isFalse();
        verifyNoInteractions(populasjonKlient, ansattGruppeKlient);
    }

    private static BeskyttetRessursAttributter attributter(IdentType identType, String brukerId) throws NoSuchMethodException {
        var metode = AnnenPartUttaksplanRest.class.getMethod("lagre", AnnenPartUttaksplanRest.AnnenPartUttaksplanRequest.class);
        var beskyttetRessurs = metode.getAnnotation(BeskyttetRessurs.class);
        return BeskyttetRessursAttributter.builder()
            .medBrukerId(brukerId)
            .medIdentType(identType)
            .medActionType(beskyttetRessurs.actionType())
            .medAvailabilityType(beskyttetRessurs.availabilityType())
            .medResourceType(beskyttetRessurs.resourceType())
            .medServicePath("/uttaksplan/annen-part")
            .medDataAttributter(AbacDataAttributter.opprett())
            .build();
    }
}
