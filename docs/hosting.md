# Hosted portfolio deployment

QIP's first hosted target is an on-demand AWS Lightsail portfolio instance. The topology remains portable Docker Compose: Caddy terminates TLS, QIP serves the packaged frontend and API, and PostgreSQL/pgvector owns relational and vector data. No database or application port is exposed directly.

This is a demonstration deployment, not a production SLA. Keep it private or start it only for scheduled reviews. QIP has bounded uploads and role checks, but it does not yet have public registration, abuse throttling, enterprise identity, multi-tenancy, or a managed recovery service. Load only the repository's visibly synthetic fixtures.

## Cost and model boundary

As of 2026-08-24, the selected Lightsail Linux bundle with 2 GB RAM, 2 vCPUs, 60 GB SSD, public IPv4, and 3 TB transfer is USD 12/month. Lightsail snapshot storage is USD 0.05/GB-month. A domain is separate. Check the [current instance bundles](https://docs.aws.amazon.com/lightsail/latest/userguide/amazon-lightsail-bundles.html) and [snapshot pricing](https://docs.aws.amazon.com/lightsail/latest/userguide/amazon-lightsail-faq-snapshots.html) before provisioning.

The hosted Compose file forces the deterministic `local` profile. It demonstrates retrieval controls, citations, review, and lifecycle behavior without credentials or inference charges; it is not representative of Ollama answer quality. The local 30B model is not suitable for this 2 GB VM. Adding a hosted model adapter is a later, separately costed security decision.

## One-time host preparation

1. Create a 2 GB Ubuntu Lightsail instance and attach a static IP. Point the chosen domain's A record at that address.
2. Allow inbound TCP 80 and 443. Keep TCP 5432 and 8080 closed. SSH uses public-key authentication, a dedicated deployment account, disabled password/root login, and the narrowest workable firewall source policy. GitHub-hosted runners do not have one stable egress address, so a permanently automated workflow requires hardened key-only SSH or a separately managed private runner/network path.
3. Install supported Docker Engine and the Compose plugin. Apply OS and Docker security updates routinely.
4. Create the deployment account and `/opt/qip`, owned by that account. Membership in the Docker group is effectively root-equivalent; protect its SSH key accordingly.
5. Copy `deploy/.env.hosted.example` to `/opt/qip/deploy/.env`, replace every placeholder with a long random value, and run `chmod 600 /opt/qip/deploy/.env`. Never place these application secrets in GitHub.
6. Enable daily Lightsail automatic snapshots. They keep the latest seven snapshots. Also schedule `/opt/qip/scripts/backup-hosted.sh` and copy its checksum-protected output to encrypted off-host storage; a snapshot alone is not an independent backup.

Caddy requires the domain to resolve to the instance and public access to ports 80 and 443. It then obtains, renews, and persists public certificates automatically.

## GitHub deployment environment

Create a GitHub environment named `portfolio`. Restrict it to release tags and require approval when the repository plan supports that protection. Add:

| Kind | Name | Value |
| --- | --- | --- |
| Variable | `QIP_HOSTED_DOMAIN` | Public DNS name, without `https://` |
| Secret | `QIP_HOSTED_HOST` | Static IP or SSH host |
| Secret | `QIP_HOSTED_USER` | Dedicated deployment account |
| Secret | `QIP_HOSTED_SSH_KEY` | Private deployment key |
| Secret | `QIP_HOSTED_KNOWN_HOSTS` | Exact, previously verified SSH host-key line |

Application passwords remain only in the mode-0600 host file. The workflow never prints or uploads it.

## Deploy and roll back

1. Publish a semantic release through the documented release process.
2. Run **Deploy hosted portfolio**, choose `deploy`, and enter its `vMAJOR.MINOR.PATCH` tag.
3. The workflow checks that the GitHub Release exists, verifies the GHCR image attestation, uploads the deployment definition from the tagged commit, pulls the immutable version tag, and waits for readiness.
4. If readiness fails, the host automatically restores the prior image. For a later regression, run the workflow with `rollback`; it swaps back to the recorded previously healthy image.

Only one deployment runs at once. Do not remove an older GHCR version while it is the rollback candidate. Compose configuration changes must remain compatible with the immediately preceding image; a database migration must follow Flyway's forward-compatible, roll-forward policy because image rollback does not reverse schema migrations.

Useful host checks:

```shell
cd /opt/qip
docker compose --env-file deploy/.env --env-file deploy/.deployment.env -f deploy/compose.hosted.yaml ps
docker compose --env-file deploy/.env --env-file deploy/.deployment.env -f deploy/compose.hosted.yaml logs --since 15m qip
curl --fail --silent https://qip.example.com/actuator/health/readiness
```

## Backup and restore drill

`backup-hosted.sh` briefly stops QIP writes, creates a PostgreSQL custom-format dump and document-volume archive, writes SHA-256 checksums, restarts QIP, and removes local backup directories older than 14 days. Schedule it during a quiet window, for example with a root-owned systemd timer. Monitor both its exit status and off-host copy.

A quarterly restore drill is required before calling the backup usable:

1. Provision an isolated replacement VM with the same deployment definition and an empty set of volumes.
2. Verify `sha256sum --check SHA256SUMS`.
3. Start PostgreSQL only, pipe the dump into `pg_restore --no-owner` using the configured database/user, and restore the document archive into `qip-hosted-document-data`.
4. Start QIP, wait for readiness, and verify document download/index state plus one synthetic investigation.
5. Destroy the isolated drill environment. Never test a destructive restore over the live volumes.

The database dump and document archive form one backup set and must be retained, encrypted, and restored together. Recovery point is the last successful off-host copy; recovery time is not guaranteed until measured by the drill.

## Demo access and shutdown

Prefer a private or scheduled demonstration. Share a non-admin synthetic account only for the review window, rotate it afterward, and retain administrator access privately. Reset the instance from a known snapshot or delete synthetic records after an untrusted session. Stop or delete the instance when ongoing availability does not justify the cost, while retaining only the backups and snapshots you intentionally need.

ADR 0014 records why this VM topology was chosen and when it should be replaced by managed persistence or a different hosting platform.
