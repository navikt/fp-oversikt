package no.nav.foreldrepenger.oversikt.integrasjoner.pdl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.kontrakter.felles.typer.Fødselsnummer;
import no.nav.foreldrepenger.oversikt.domene.AktørId;
import no.nav.foreldrepenger.oversikt.saker.BrukerIkkeFunnetIPdlException;
import no.nav.pdl.HentIdenterQueryRequest;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.IdentGruppe;
import no.nav.pdl.IdentInformasjon;
import no.nav.pdl.Identliste;
import no.nav.pdl.IdentlisteResponseProjection;
import no.nav.pdl.PersonResponseProjection;
import no.nav.vedtak.felles.integrasjon.person.PdlException;

/**
 * Tester oversettelsen fra PDL-feil til unntak i aktørId-oppslaget. Kallere skiller ukjent ident
 * (BrukerIkkeFunnetIPdlException) fra tekniske feil (PdlException) for ikke å avsløre om en ident finnes i PDL.
 * Fnr-ene er unike per test fordi klassen har en statisk cache.
 */
class PdlKlientSystemTest {

    private PdlKlientSystem klient;

    @BeforeEach
    void setUp() {
        // Mock med ekte metoder: kjører aktørId og fp-felles sin hentId, men stubber selve GraphQL-kallet.
        klient = mock(PdlKlientSystem.class, CALLS_REAL_METHODS);
    }

    @Test
    void ukjent_ident_i_pdl_gir_BrukerIkkeFunnetIPdlException() {
        doThrow(pdlFeil(HttpURLConnection.HTTP_NOT_FOUND)).when(klient)
            .hentIdenter(any(HentIdenterQueryRequest.class), any(IdentlisteResponseProjection.class));

        assertThatThrownBy(() -> klient.aktørId(new Fødselsnummer("11111111111"))).isInstanceOf(BrukerIkkeFunnetIPdlException.class);
    }

    @Test
    void teknisk_feil_fra_pdl_propageres_som_PdlException() {
        var feil = pdlFeil(HttpURLConnection.HTTP_INTERNAL_ERROR);
        doThrow(feil).when(klient).hentIdenter(any(HentIdenterQueryRequest.class), any(IdentlisteResponseProjection.class));

        assertThatThrownBy(() -> klient.aktørId(new Fødselsnummer("22222222222"))).isSameAs(feil);
    }

    @Test
    void kjent_ident_gir_aktørId() throws BrukerIkkeFunnetIPdlException {
        var ident = new IdentInformasjon();
        ident.setIdent("1234567890123");
        ident.setGruppe(IdentGruppe.AKTORID);
        var identliste = new Identliste();
        identliste.setIdenter(List.of(ident));
        doReturn(identliste).when(klient).hentIdenter(any(HentIdenterQueryRequest.class), any(IdentlisteResponseProjection.class));

        assertThat(klient.aktørId(new Fødselsnummer("33333333333"))).isEqualTo(new AktørId("1234567890123"));
    }

    @Test
    void ukjent_barn_i_pdl_har_ikke_disse_foreldrene() {
        // Stubber oppslaget uten ignoreNotFound, slik at 404-håndteringen i fp-felles sin hentPerson kjøres.
        doThrow(pdlFeil(HttpURLConnection.HTTP_NOT_FOUND)).when(klient)
            .hentPerson(any(HentPersonQueryRequest.class), any(PersonResponseProjection.class));

        assertThat(klient.barnHarDisseForeldrene(new Fødselsnummer("44444444444"), new Fødselsnummer("55555555555"),
            new Fødselsnummer("66666666666"))).isFalse();
    }

    @Test
    void teknisk_feil_fra_pdl_ved_oppslag_av_barn_propageres() {
        var feil = pdlFeil(HttpURLConnection.HTTP_INTERNAL_ERROR);
        doThrow(feil).when(klient)
            .hentPerson(any(HentPersonQueryRequest.class), any(PersonResponseProjection.class));

        assertThatThrownBy(() -> klient.barnHarDisseForeldrene(new Fødselsnummer("77777777777"), new Fødselsnummer("55555555555"),
            new Fødselsnummer("66666666666"))).isSameAs(feil);
    }

    private static PdlException pdlFeil(int status) {
        return new PdlException("F-TEST", List.of(), null, status, URI.create("http://pdl.test/graphql"));
    }
}
