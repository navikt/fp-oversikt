# fp-oversikt

Citizen-facing backend for case, document, and benefit timeline overview.

## Shared context

- Source of truth for shared domain, architecture, and conventions: `navikt/fp-context`
- Copilot Space: `navikt/TeamForeldrepenger`

## Repo-specific context

| Topic              | Details                                                                             |
|--------------------|-------------------------------------------------------------------------------------|
| Role               | Serves structured personal and saksoversikt data to self-service clients            |
| Consumers          | `forelderepengesoknad` (all logged-in apps) and `fp-soknad` (lookup)                |
| Tech stack         | Standard fp Java backend using `fp-prosesstask`                                     |
| Main integrations  | `fp-sak`, SAF, PDL, Aa-register, Brreg, EREG                                        |
| Data               | PostgreSQL holding a cahced view of upstream data (sak, applications, vedtak, docs) |

Specials:
- Citizen-facing using TokenX. Authorization pr endpoint using TilgangKontrollTjeneste.
- Serves personal data needed in søknad dialogues, when submitting and for later status/sak overview.
- Serves saksoversikt data (status, results, facts) to app `foreldrepengeoversikt`

## Entry points

Start with server/konfig/ApiConfig and registered local classes to get all endpoints.

## Verification

- For integration impact, verify via `navikt/fp-autotest`.
- Most relevant suite: `verdikjede`.
