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

1. **Lagre søknadsplanen.** `fp-soknad` sender perioder og eksplisitt `søknadType`. En førstegangssøknad erstatter hele søknadsplanen. Endringssøknader får periodene før første innsendte `fom` fra fellesplanen for saken. Perioder som krysser datoen kappes, og innsendte perioder erstatter resten. Manglende eller tomt grunnlag, inkludert EØS-only, gir ingen lagring. Innsendinger som ikke er nyere enn aktiv søknad, ignoreres.
2. **Finn sakene for barnet.** Ved henting kontrolleres tilgang, og sakene matches på barn eller familiehendelse. Annen parts sak brukes bare når koblingen til søkeren er gyldig. Skjermet eller ukjent annen part utelates fra saksoppslaget.
3. **Velg søknadsgrunnlag.** Søkerens aktive søknad brukes når søkeren har sak, ellers annen parts aktive søknad. Søknaden kan inneholde perioder for begge foreldre. Uten aktiv søknad brukes søkerens egen legacy-søknad som reserve, aldri annen parts legacy-søknad.
4. **Prioriter vedtak.** For hver forelder brukes gjeldende vedtak fremfor søknadsuttak. Uten vedtak brukes uttak fra søknadsgrunnlaget. Arbeidsgiveropplysninger fjernes fra annen parts gradering.
5. **Skjerm og bygg tidslinjen.** `UttaksplanTjeneste.hentFor` skjuler annen parts uttak, utsettelser og EØS-perioder ved adressebeskyttelse, uavklart identitet eller avvik mellom request og sak. Dette gjelder også perioder fra søkerens egen søknad. Filtreringen skjer før normalisering av overlapp og samtidig uttak. Søknadsutsettelser fyller bare hull i tidslinjen.
6. **Returner planen.** Periodene returneres med søker og annen part i hvert sitt felt, sammen med termindato, antall barn og dekningsgrad. Uten sak for noen av foreldrene returneres ingen plan.

Lagringen ligger i `SøknadsplanTjeneste`. `UttaksplanRest`, `UttaksplanTjeneste` og `UttaksplanTidslinje` håndterer henting og oppbygging.

https://confluence.adeo.no/pages/viewpage.action?pageId=529061042
