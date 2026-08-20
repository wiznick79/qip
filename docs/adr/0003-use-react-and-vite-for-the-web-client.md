# ADR 0003: Use React and Vite for the web client

- Status: Accepted
- Date: 2026-08-20

## Context

Assets, incidents, and document ingestion now form a stable backend workflow that benefits from a browser interface. The client must remain thin: it calls QIP's REST API, renders domain state and validation failures, and later grows into an investigation workspace with questions, citations, and source navigation. It does not need server-side rendering, independent deployment, a separate backend-for-frontend, or direct access to persistence and AI providers.

The spike considered React, Vue, and a framework-free TypeScript client. All can satisfy the first screens. The expected investigation workspace will have several interacting panels and stateful workflows, while portfolio value and maintainability favor a widely understood component model. A framework-free client would initially reduce dependencies but would require application-specific rendering and state conventions as that workspace grows.

## Decision

Use React 19 with strict TypeScript and Vite 8. Keep the application client-rendered and use the official Vite React plugin. Vite's development proxy sends `/api` calls to Spring Boot; its production build produces static assets that Maven packages under Spring Boot's `static` classpath location.

Run TypeScript checking separately because Vite transpiles TypeScript without type-checking. Use Vitest with Testing Library for behavior tests. The CI workflow runs `npm ci` and the frontend verification before the Maven build, ensuring the packaged JAR receives a verified production bundle.

Do not add a router, global state library, component kit, CSS framework, server rendering, or a separate deployment in this increment. Three top-level workspace views use local component state and the native Fetch API. Reconsider those dependencies only when navigation, shared state, or accessibility requirements demonstrate the need.

## Alternatives considered

### Vue with Vite

Vue provides an equally credible typed component model and excellent Vite integration. React was selected because the future investigation workspace maps naturally to its composition model and it is more immediately recognizable in this portfolio. This is an ecosystem and communication choice, not a claim that Vue is technically incapable.

### Framework-free TypeScript

Native web APIs would minimize dependencies for static forms, but manual rendering and state synchronization would become application infrastructure once citations, passage panels, and question history arrive.

### Next.js or another server-rendered React framework

QIP is an authenticated-style application workspace rather than public content requiring search indexing or server rendering. Adding a Node production server would create a second runtime and deployment boundary without an MVP need.

## Consequences

- Frontend code lives under `frontend/` but shares the repository, CI, JAR, and release lifecycle.
- Local UI development requires a supported Node.js version in addition to Java and Docker.
- The browser depends only on documented `/api` contracts.
- Static assets are absent from a backend-only build unless the frontend has first been built; CI always performs both stages.
- React and npm dependencies require routine security and upgrade review.

## Reevaluation triggers

Add a router when deep links or browser history become user requirements. Add shared state only when state genuinely crosses several screens. Revisit independent hosting when frontend release cadence, caching, ownership, or scaling differs materially from the backend.

## References

- [React TypeScript guide](https://react.dev/learn/typescript)
- [Vite guide](https://vite.dev/guide/)
- [Vite TypeScript behavior](https://vite.dev/guide/features.html#typescript)
