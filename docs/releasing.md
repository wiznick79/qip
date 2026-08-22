# Release process

QIP uses semantic `vMAJOR.MINOR.PATCH` Git tags as the release boundary. A tag triggers `.github/workflows/release.yml`; ordinary commits and pull requests never publish packages.

## Before tagging

1. Start from a clean `main` synchronized with `origin/main`.
2. Confirm the Build workflow is green.
3. Move the relevant entries in `CHANGELOG.md` from `Unreleased` to a dated version heading.
4. Confirm `pom.xml` declares either the matching version or its `-SNAPSHOT` form.
5. Run `npm ci && npm run verify` in `frontend/` and `./mvnw clean verify` at the repository root.
6. Build and smoke-test the production stack with `docker compose up --build -d --wait`.

## Publish

Create and push an annotated tag only after the checklist passes:

```shell
git tag -a v0.1.0 -m "QIP v0.1.0"
git push origin v0.1.0
```

The release workflow verifies that the tag matches the Maven project version, rebuilds the frontend and backend, and publishes:

- `qip-<version>.jar` and `SHA256SUMS` on the GitHub Release;
- `ghcr.io/wiznick79/qip:<version>`;
- matching `<major>.<minor>` and `latest` container tags;
- GitHub build-provenance attestations for both the JAR and image.

The workflow does not publish database data, uploaded documents, Ollama models, prompts, or local configuration.

## Verify published artifacts

```shell
gh release download v0.1.0
sha256sum --check SHA256SUMS
gh attestation verify qip-0.1.0.jar -R wiznick79/qip
docker pull ghcr.io/wiznick79/qip:0.1.0
gh attestation verify oci://ghcr.io/wiznick79/qip:0.1.0 -R wiznick79/qip
```

On PowerShell, compare `Get-FileHash .\qip-0.1.0.jar -Algorithm SHA256` with the value in `SHA256SUMS`.

The published image can also be used with the repository's Compose topology:

```powershell
$env:QIP_IMAGE = "ghcr.io/wiznick79/qip:0.1.0"
docker compose pull qip
docker compose up -d --no-build --wait
```

## Failure and rollback

Do not move or reuse a published version tag. Correct the problem on `main`, increment the patch version, update the changelog, and create a new tag. A bad container release may be documented as withdrawn, but its provenance remains auditable.
