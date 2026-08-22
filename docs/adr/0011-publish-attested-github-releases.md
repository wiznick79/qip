# ADR 0011: Publish attested GitHub releases and GHCR images

- Status: Accepted
- Date: 2026-08-22

## Context

QIP can be built locally, but a portfolio reviewer has no immutable artifact, checksum, container registry path, or traceable release process. Publishing directly from ordinary branch builds would make provenance ambiguous and grant unnecessary write permissions to pull-request workflows.

## Decision

Semantic `vMAJOR.MINOR.PATCH` tags trigger a dedicated GitHub Actions workflow. The workflow verifies the tag against the Maven project version, rebuilds and tests the exact tagged commit, publishes a versioned executable JAR with a SHA-256 checksum, publishes the single QIP application image to GitHub Container Registry, and creates GitHub build-provenance attestations for both artifacts.

Pull requests retain read-only permissions and must verify both the application and production image without publishing either. Releases never contain database contents, uploads, local configuration, or Ollama models.

## Alternatives considered

- **Attach only a JAR manually:** simpler, but manual builds are not reproducible or attributable and omit the supported container experience.
- **Publish every `main` build:** useful for continuous delivery, but unnecessary for a portfolio MVP and makes the meaning of a release unclear.
- **Introduce a cloud deployment platform:** provides a live URL but adds credentials, cost, networking, and operational scope before the local product is release-ready.

## Consequences

- Consumers can download a checksum-protected JAR or pull a versioned image and verify its GitHub provenance.
- The release workflow needs scoped `contents`, `packages`, `id-token`, and `attestations` write permissions; normal CI remains read-only.
- A release is immutable. Corrections require a new version rather than moving a tag.
- Release artifacts carry the repository's Apache-2.0 license metadata; a maintainer must still create each release tag deliberately.

## Reevaluation trigger

Reconsider the delivery path when QIP gains a hosted demonstration, multiple deployable services, signed platform installers, or a release cadence that justifies promotion across environments.
