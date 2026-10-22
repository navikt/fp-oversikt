# fp-oversikt

[![Bygg og deploy](https://github.com/navikt/fp-oversikt/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/navikt/fp-oversikt/actions/workflows/build.yml)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=navikt_fp-oversikt&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=navikt_fp-oversikt)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=navikt_fp-oversikt&metric=coverage)](https://sonarcloud.io/summary/new_code?id=navikt_fp-oversikt)

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=navikt_fp-oversikt&metric=alert_status)](https://sonarcloud.io/dashboard?id=navikt_fp-oversikt)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=navikt_fp-oversikt&metric=bugs)](https://sonarcloud.io/dashboard?id=navikt_fp-oversikt)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=navikt_fp-oversikt&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=navikt_fp-oversikt)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=navikt_fp-oversikt&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=navikt_fp-oversikt)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=navikt_fp-oversikt&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=navikt_fp-oversikt)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=navikt_fp-oversikt&metric=sqale_index)](https://sonarcloud.io/dashboard?id=navikt_fp-oversikt)


Backend tjenester for saksoversikt for foreldrepenger

## Felles uttaksplan

Planen bygges fra lagrede søknadsperioder og gjeldende vedtak, sett fra den innloggede søkerens side.

1. **Lagre søknadsplanen.** `fp-soknad` sender perioder og eksplisitt `søknadType`. En førstegangssøknad erstatter hele søknadsplanen. En endringssøknad beholder periodene før første endringsdato og erstatter resten. Tjenesten ignorerer innsendinger som ikke er nyere enn den lagrede søknaden, og endringssøknader uten lagret søknad.
2. **Finn sakene for barnet.** Ved henting kontrolleres tilgang, og sakene matches på barn eller familiehendelse. Annen parts sak brukes bare når koblingen til søkeren er gyldig. Skjermet eller ukjent annen part utelates fra saksoppslaget.
3. **Velg søknadsgrunnlag.** Søkerens aktive søknad brukes når søkeren har sak, ellers annen parts aktive søknad. Søknaden kan inneholde perioder for begge foreldre. Uten aktiv søknad brukes søkerens egen legacy-søknad som reserve, aldri annen parts legacy-søknad.
4. **Prioriter vedtak.** For hver forelder brukes gjeldende vedtak fremfor søknadsuttak. Uten vedtak brukes uttak fra søknadsgrunnlaget. Arbeidsgiveropplysninger fjernes fra annen parts gradering.
5. **Bygg tidslinjen.** Periodene for begge foreldre og eventuelle EØS-perioder settes sammen. Overlapp og samtidig uttak normaliseres, og søknadsutsettelser fyller bare hull i tidslinjen.
6. **Returner planen.** Periodene returneres med søker og annen part i hvert sitt felt, sammen med termindato, antall barn og dekningsgrad. Uten sak for noen av foreldrene returneres ingen plan.

Lagringen ligger i `SøknadTjeneste`. `UttaksplanRest`, `UttaksplanTjeneste` og `UttaksplanTidslinje` håndterer henting og oppbygging.

https://confluence.adeo.no/pages/viewpage.action?pageId=529061042
