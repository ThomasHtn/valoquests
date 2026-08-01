# 0007. Protect write operations with a shared admin key

## Status

Accepted

## Context

The application has two kinds of route. Reads expose a group's Valorant statistics: challenge progress, rankings,
player profiles, match history. Writes trigger synchronizations and recalculations - operations that call a rate-limited
external API and rebuild derived data.

There are no user accounts. Nobody signs up, nobody owns a profile, and the six tracked players are seeded by
migration. Introducing authentication would mean inventing a user concept the domain does not have, purely to protect
four administrative endpoints operated by one person.

## Decision

Reads are public. Writes require a shared secret in the `X-Admin-Key` header.

```java
.requestMatchers(HttpMethod.GET, "/api/**").permitAll()
.requestMatchers("/api/admin/**").permitAll()   // delegated to AdminApiKeyFilter
.anyRequest().denyAll()
```

`AdminApiKeyFilter` runs before the authorization rules and short-circuits any request whose path starts with
`/api/admin` without a valid key. It answers a structured error - `ADMIN_KEY_MISSING` or `ADMIN_KEY_INVALID` - rather
than a bare 401 body.

Supporting rules:

- The key arrives through `ADMIN_API_KEY` and is required at startup; it is never committed and never logged.
- Sessions are stateless and CSRF is disabled, which is coherent because no cookie carries authority.
- CORS allows exactly one origin, configured through `FRONTEND_ORIGIN`, and explicitly whitelists the `X-Admin-Key`
  header.
- Anything not matched by a rule is denied, so a new endpoint is closed by default.

## Consequences

- The frontend needs no authentication flow, no token storage and no refresh logic. It only ever issues reads.
- Swagger UI exposes the key through its **Authorize** action, so administrative routes stay explorable during
  development.
- A single shared secret has no revocation granularity and no audit trail of who used it. That is acceptable for one
  operator and would not be for a team: rotating the key means redeploying with a new value.
- The key is a bearer secret in a header, so it is only as safe as the transport. Deploying the API without TLS would
  expose it.
- Should per-user authorization ever be needed, the filter is a single component to replace and the route matchers
  already separate reads from writes.
