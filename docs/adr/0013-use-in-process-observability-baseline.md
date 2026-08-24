# ADR 0013: Use an in-process observability baseline

- Status: accepted
- Date: 2026-08-24

## Context

Document extraction, embedding, retrieval, and local model generation are now the slowest and most failure-prone parts of QIP. Persisted statuses show the final result, but they do not show stage latency, aggregate failure patterns, or which safe log events belong to one HTTP request. QIP still has one process and one deployment boundary, so an external telemetry stack would add operational cost before there is a hosted retention, alerting, or cross-service tracing requirement.

Operational data must not expose uploaded content, prompts, generated responses, credentials, authorization values, or identifiers as unbounded metric tags.

## Decision

Use the Micrometer registry already supplied by Spring Boot Actuator for low-cardinality application timers and counters. Measure ingestion stages, retrieval, model calls, and terminal answer outcomes with fixed enum-like tags only. Eagerly register every bounded series and tag combination at startup so documented endpoints are discoverable before the first operation.

Use Spring Boot's structured Logstash console format. At the servlet boundary, accept only a bounded safe `X-Correlation-ID` or create a UUID, return it in the response, and retain it in MDC for the request lifetime. Emit one completion event with method, URI path, status, and duration; do not log query strings or payloads.

Expose only Actuator health and metrics. Health remains public for container probes with details hidden. Metrics require QIP's administrator role. Provide an administrator-only web page that reads those secured endpoints and summarizes current-process counts, failure ratios, latency, and answer outcomes without adding persistence. Document local service-level indicators and a bounded troubleshooting sequence.

## Alternatives considered

### OpenTelemetry collector and distributed tracing

This would provide durable traces and standardized export, but QIP has no cross-process call chain to trace and no hosted collector. Adding it now would teach configuration rather than solve a measured diagnostic gap.

### Prometheus and Grafana in Compose

Dashboards and history are useful once operators need retention and alerts. Bundling two more services into the default local demo would increase startup, storage, and maintenance cost for metrics that Actuator can already expose on demand.

### Log-only diagnostics

Logs can correlate individual failures but are poor at quantifying latency and outcome trends. In-process counters and timers provide that aggregate view without a new runtime service.

## Consequences

- Local operators can distinguish ingestion, retrieval, and model problems without viewing sensitive content.
- Metric cardinality remains bounded because IDs, users, and model tags are excluded.
- All bounded metric series are present with zero values at startup; values reset when the process restarts and there is no built-in history or alerting.
- JSON logs are machine-readable and less compact than plain development logs.
- Administrator credentials are required to inspect metrics or access the in-application Operations page when security is enabled.

## Re-evaluation trigger

Add OpenTelemetry export or an external metrics/log stack when hosted operation requires retention, alerting, correlation across independently deployed processes, or evidence that console logs and on-demand metrics no longer support incident diagnosis.
