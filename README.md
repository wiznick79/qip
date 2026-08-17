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

The integration test suite uses Testcontainers and therefore requires a running Docker engine.

## Local database

The local dependency is PostgreSQL 17 with pgvector 0.8.6. Start it with:

```shell
docker compose up -d --wait
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

On Windows PowerShell, run the application with:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

The `local` profile uses deliberately non-secret development defaults from `application-local.yml`. Copy `.env.example` to `.env` only when you need to override them. Other environments should provide standard Spring datasource configuration through secret management rather than activate the `local` profile.

Stop the database with `docker compose down`. Its named volume is preserved; add `--volumes` only when you intentionally want to delete local database data.

The repository currently contains the milestone 2 persistence foundation. No domain behavior or AI integration has been implemented yet; see the implementation sequence in the architecture document.
