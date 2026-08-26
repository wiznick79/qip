# ADR 0014: Deploy the portfolio demo to a single Lightsail instance

- Status: Accepted
- Date: 2026-08-24

## Context

QIP now publishes a tested, attested application image, but a portfolio reviewer still needs to build it locally. A hosted demonstration must preserve PostgreSQL/pgvector data and uploaded documents, terminate TLS, keep credentials out of the repository, support rollback, and remain inexpensive enough to run selectively. QIP does not yet need independent scaling, high availability, or multiple deployment units.

The local Ollama models are too large for an economical portfolio VM and would make response latency and memory requirements dominate the hosting choice. The deterministic adapters are safe and credential-free but demonstrate workflow and provenance rather than production-quality semantic synthesis.

## Decision

Use one 2 GB AWS Lightsail Linux instance as the first documented target. Run Caddy, the released QIP image, and the pinned PostgreSQL/pgvector image through a dedicated hosted Compose file. Publish only ports 80 and 443; Caddy obtains and renews TLS certificates and proxies to QIP on the private Compose network. PostgreSQL is never published.

Keep PostgreSQL data, uploaded documents, and Caddy state in separate named volumes. Keep application and database credentials only in a mode-0600 host environment file. GitHub's `portfolio` environment stores only the SSH deployment credentials and exact known-host entry. The deployment workflow accepts semantic release tags, verifies the GHCR image's GitHub attestation, uploads the versioned deployment configuration, and health-checks the new image. An unhealthy deployment automatically restores the prior image; an explicit workflow operation performs the same rollback later.

The hosted demonstration uses the deterministic `local` profile and visibly synthetic data. It does not claim production AI quality. Keep it private or start it only for scheduled reviews because the current application does not include public-service abuse controls or enterprise identity. Ollama remains a local evaluation option; a hosted model provider requires a separate cost, privacy, credential, and adapter decision.

## Alternatives considered

- **Render or a similar application platform:** lower host maintenance, but a web service, persistent disk, and durable PostgreSQL are separately billed and make document/database backup behavior platform-specific.
- **A DigitalOcean Droplet or another Docker VM:** technically equivalent and remains portable because the deployment uses standard Compose. Lightsail was selected for its predictable bundled price and recognizable portfolio operations story.
- **AWS ECS/Fargate with managed PostgreSQL and object storage:** stronger service isolation and managed persistence, but materially more infrastructure, IAM, networking, and monthly cost than this single-user demonstration requires.
- **Host Ollama beside QIP:** requires substantially more memory and usually GPU capacity. It is not economical for an intermittently used portfolio URL.
- **Kubernetes:** adds no useful capability for one application replica and one database.

## Consequences

- The complete demonstration has one small host, one DNS record, and a predictable baseline cost.
- The host remains a single point of failure and needs OS/Docker patching, disk monitoring, and tested off-host backups.
- Deployments are repeatable and provenance-checked, but host provisioning is deliberately documented rather than hidden behind a cluster platform.
- The Compose design can move to another Linux VM without changing application architecture.
- Public access must remain authenticated and contain only synthetic data; the operator may shut the instance down between reviews.

## Reevaluation triggers

Adopt managed PostgreSQL/object storage, a hosted model provider, or a multi-instance platform only when uptime, concurrent usage, data durability, or model-quality requirements justify their additional recurring cost and operations. Revisit Lightsail if measured memory pressure exceeds the 2 GB plan or a private/on-demand demo proves more appropriate.

## References

- [Amazon Lightsail instance bundles](https://docs.aws.amazon.com/lightsail/latest/userguide/amazon-lightsail-bundles.html)
- [Amazon Lightsail snapshot behavior and pricing](https://docs.aws.amazon.com/lightsail/latest/userguide/amazon-lightsail-faq-snapshots.html)
- [Amazon Lightsail firewall guidance](https://docs.aws.amazon.com/lightsail/latest/userguide/understanding-firewall-and-port-mappings-in-amazon-lightsail.html)
- [Caddy automatic HTTPS requirements](https://caddyserver.com/docs/automatic-https)
- [GitHub deployment environments](https://docs.github.com/en/actions/reference/workflows-and-actions/deployments-and-environments)
