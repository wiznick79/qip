# ADR 0012: Authenticate human actions with Spring Security

- Status: Accepted
- Date: 2026-08-23

## Context

QIP preserves who authored observations, submitted evidence, reviewed findings, and closed investigations. In v0.1.0 those values are caller-supplied provenance labels. They are useful for demonstrating the workflow but are not trustworthy identities: any client can claim another name.

The first security increment needs to protect the integrity of human decisions without prematurely adding enterprise identity, a user-management subsystem, multi-tenancy, or fine-grained plant authorization.

## Decision

Use Spring Security at the application boundary with session-based browser authentication.

Local development provides a small configurable set of synthetic users with the roles `INVESTIGATOR`, `REVIEWER`, and `ADMIN`. Passwords are configuration inputs; non-local environments must supply them through secret management. The browser authenticates through QIP and retains the server-managed session. API authorization derives actor identity from the authenticated principal rather than accepting identity fields in request bodies.

Finding review requires `REVIEWER` or `ADMIN`. Investigation closure requires `INVESTIGATOR` or `ADMIN`. Other mutating human actions require an authenticated user and record that principal. Read access remains authenticated in this increment so incident and document content is not anonymously exposed.

CSRF protection remains enabled for the session-authenticated browser. QIP exposes a bounded session endpoint that returns the current user, roles, and a CSRF token for subsequent state-changing requests. Authentication and authorization failures use safe RFC 9457-compatible responses and do not expose credentials or internal details.

## Consequences

- Stored human attribution becomes a trustworthy application identity within the configured local identity store.
- The React client must handle login, logout, expired sessions, CSRF tokens, and role-sensitive controls.
- Scripted demonstrations must authenticate before calling protected endpoints.
- Existing database columns and historical attribution remain valid provenance snapshots; no user table or foreign key is introduced yet.
- In-memory/configured local users are not a production identity solution. Replacing them with OIDC can preserve the principal-and-role boundary without changing business modules.

## Alternatives considered

### HTTP Basic authentication

It is simple for scripts but provides a poorer browser logout/session experience and repeatedly sends credentials. It remains useful only as a possible non-browser integration mechanism later.

### JWT tokens issued by QIP

Self-issued tokens would add signing-key lifecycle, refresh, revocation, and browser-storage decisions without an external identity provider or distributed-service need.

### OIDC immediately

OIDC is the likely hosted or enterprise direction, but requiring an external provider would make the local portfolio demonstration harder and expand this milestone into identity provisioning.
