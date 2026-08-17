# Quality Investigation Platform (QIP)

QIP is a standalone, AI-assisted platform for investigating industrial incidents and quality problems. It combines user-entered incident data with uploaded technical knowledge, then helps investigators find and summarize relevant evidence while preserving the source of every result.

The project is intentionally starting as a modular Java application. Kafka, microservices, Kubernetes, and cloud deployment are later learning milestones that must be justified by an actual architectural need.

Start with:

- [Architecture and MVP](docs/architecture.md)
- [Engineering instructions](AGENTS.md)
- [ADR 0001: modular monolith and single Maven module](docs/adr/0001-modular-monolith-and-single-maven-module.md)

## Development

Prerequisites: Java 21 or newer. Use the committed Maven Wrapper for all project commands.

```shell
./mvnw verify
```

On Windows PowerShell:

```powershell
.\mvnw.cmd verify
```

Apply the Java formatter with `./mvnw spotless:apply`. The `verify` lifecycle compiles the application, runs tests, verifies Spring Modulith boundaries, checks formatting, and runs the static-analysis baseline.

The repository currently contains the milestone 1 application skeleton. No domain behavior, persistence, AI integration, or external infrastructure has been implemented yet; see the implementation sequence in the architecture document.
