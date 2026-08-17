# ADR 0001: Start as a modular monolith in one Maven module

- Status: Accepted
- Date: 2026-08-17

## Context

QIP must support incident management, document knowledge retrieval, and grounded AI-assisted investigation. These capabilities have meaningful domain boundaries, but the MVP has one development team, one deployment lifecycle, modest load, and no demonstrated need for independent scaling or distributed transactions.

The project also needs those boundaries to be visible and testable so that a future service extraction is based on evidence rather than a technology goal.

## Decision

Build QIP as one Spring Boot deployable and one Maven module using Java packages as business-module boundaries. The initial business modules are `assets`, `incidents`, `knowledge`, and `investigations`.

Use Spring Modulith to describe allowed dependencies, named public interfaces, and to verify the package arrangement in the default build. Keep implementations internal to their owning module. Cross-module calls use deliberate public APIs or application events.

Keep the Maven build as one module. Maven submodules will be introduced only when they provide a concrete build or lifecycle benefit; they are not a substitute for domain boundaries.

## Alternatives considered

### Multiple microservices immediately

This would expose network, deployment, observability, data-consistency, and test complexity before the domain or scaling characteristics are known. It was rejected for the MVP.

### One Maven submodule per domain module

Compile-time isolation is useful, but an early multi-module build adds dependency and build configuration overhead. Spring Modulith already verifies the boundaries we currently need. This can be reconsidered independently from service extraction.

### Unstructured layered monolith

Global controller/service/repository layers are initially simple but make domain ownership and coupling difficult to see. This conflicts with the intended architectural learning path and was rejected.

## Consequences

- Local development, testing, deployment, and transactions remain simple.
- Domain boundaries are explicit and continuously verified.
- A careless public module API can still create coupling, so API design and dependency tests remain important.
- Runtime isolation and independent scaling are unavailable until a module is extracted.
- Package moves may be required if a module is later extracted, but its application-owned APIs reduce that cost.

## Reevaluation triggers

Revisit this decision when a module has a demonstrably different scaling, availability, security, release, or technology lifecycle; when build times require independent builds; or when team ownership makes independent delivery materially valuable. Record service extraction in a new ADR rather than replacing this history.
