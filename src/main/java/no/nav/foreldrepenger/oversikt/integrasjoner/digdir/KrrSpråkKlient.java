package no.nav.foreldrepenger.oversikt.integrasjoner.digdir;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriBuilderException;
import no.nav.foreldrepenger.common.oppslag.dkif.Målform;
import no.nav.vedtak.felles.integrasjon.rest.NavHeaders;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.util.LRUCache;

/*
 * https://github.com/navikt/digdir-krr
 * https://digdir-krr-proxy.intern.dev.nav.no/swagger-ui/index.html#/personer-controller/postPersoner
 */
public abstract class KrrSpråkKlient {
    private static final Logger LOG = LoggerFactory.getLogger(KrrSpråkKlient.class);
    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(60, TimeUnit.MINUTES);
    private static final LRUCache<String, Kontaktinformasjoner.Kontaktinformasjon> KONTAKTINFORMASJON_CACHE = new LRUCache<>(2000, CACHE_ELEMENT_LIVE_TIME_MS);

    private final URI endpoint;
    private final RestClient restClient;
    private final RestConfig restConfig;

    KrrSpråkKlient(RestClient restClient, RestConfig restConfig) {
        this.restClient = restClient;
        this.restConfig = restConfig;
        this.endpoint = UriBuilder.fromUri(restConfig.endpoint())
            .queryParam("inkluderSikkerDigitalPost", "false")
            .build();
    }

    public Målform finnSpråkkodeMedFallback(String fnr) {
        try {
            var personOpt = hentKontaktinformasjon(fnr);
            if (personOpt.isEmpty()) {
                return Målform.NB;
            }
            var person = personOpt.get();
            if (!person.aktiv()) {
                LOG.info("KrrSpråkKlient: bruker er inaktiv, returnerer default");
                return Målform.NB;
            }
            if (person.spraak() == null) {
                LOG.info("KrrSpråkKlient: bruker har ikke språk, returnerer default");
                return Målform.NB;
            }
            if (Arrays.stream(Målform.values()).noneMatch(m -> m.name().equalsIgnoreCase(person.spraak()))) {
                LOG.info("KrrSpråkKlient: bruker har språk {}, bruker NB", person.spraak());
                return Målform.NB;
            }
            return Målform.valueOf(person.spraak().toUpperCase());
        } catch (UriBuilderException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Utviklerfeil syntax-exception for KrrSpråkKlient.finnSpråkkodeForBruker", e);
        } catch (Exception e) {
            LOG.info("KrrSpråkKlient: kall til digdir krr feilet, returnerer default", e);
            return Målform.NB;
        }
    }

    public Optional<Kontaktinformasjoner.Kontaktinformasjon> hentKontaktinformasjon(String fnr) {
        return  Optional.ofNullable(KONTAKTINFORMASJON_CACHE.get(fnr))
                .or(() -> {
                    var kontaktinformasjonOpt = hentKontaktinformasjonFraKRR(fnr);
                    kontaktinformasjonOpt.ifPresent(k -> KONTAKTINFORMASJON_CACHE.put(fnr, k));
                    return kontaktinformasjonOpt;
                });
    }

    private Optional<Kontaktinformasjoner.Kontaktinformasjon> hentKontaktinformasjonFraKRR(String fnr) {
        var request = RestRequest.newPOSTJson(new Personidenter(List.of(fnr)), endpoint, restConfig)
                .otherCallId(NavHeaders.HEADER_NAV_CALL_ID)
                .timeout(Duration.ofSeconds(3)); // Kall langt avgårde - blokkerer ofte til 3*timeout. Request inn til fpsak har timeout 20s.
        var respons = restClient.send(request, Kontaktinformasjoner.class);
        if (respons.feil() != null && !respons.feil().isEmpty()) {
            var feilkode = respons.feil().get(fnr);
            if (Kontaktinformasjoner.FeilKode.person_ikke_funnet.equals(feilkode)) {
                LOG.info("KrrSpråkKlient: fant ikke bruker, returnerer default");
            } else {
                LOG.warn("KrrSpråkKlient: Uventet feil ved kall til KRR {}, returnerer default", feilkode);
            }
            return Optional.empty();
        }
        return Optional.of(respons.personer().get(fnr));
    }

    record Personidenter(List<String> personidenter) {
    }
}
