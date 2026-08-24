# Local operations and troubleshooting

QIP exposes a deliberately small operational surface for diagnosing document ingestion and grounded-answer behavior. It uses structured console logs and in-process Micrometer metrics; it does not require or bundle an external observability stack.

## Correlation and safe logs

Every HTTP response includes `X-Correlation-ID`. A caller may supply an identifier containing 1–64 letters, digits, dots, underscores, or hyphens; QIP replaces missing or invalid values with a UUID. The same value is attached to logs produced during that request.

Console output uses Spring Boot's Logstash JSON format by default. Each request-completion event contains only bounded operational fields: correlation ID, method, URI path, status, and duration. Query strings, request/response bodies, document text, prompts, model responses, credentials, and authorization headers are not logged.

## Metrics

Health remains public with component details hidden. The metrics index and individual measurements require an authenticated `ADMIN` session.

| Metric | Tags | Meaning |
| --- | --- | --- |
| `qip.knowledge.ingestion` | `stage=extraction|indexing`, `outcome=success|failure` | Count and duration of each ingestion stage. |
| `qip.knowledge.retrieval` | `outcome=success|failure` | Count and duration of query embedding plus passage search. |
| `qip.investigations.model` | `outcome=success|failure` | Count and latency of answer-model calls. Insufficient retrieval does not call the model. |
| `qip.investigations.answers` | `status=GROUNDED|INSUFFICIENT_EVIDENCE|TECHNICAL_FAILURE` | Persisted terminal answer outcomes. |

Tags are fixed enums. Document, incident, investigation, user, model, and correlation identifiers are intentionally absent to prevent sensitive or unbounded metric cardinality. Every documented series and tag combination is registered at startup, so each individual metric endpoint exists with zero-valued measurements before the first operation. Values are in-process and reset when QIP restarts.

Administrators can inspect the same measurements in the web application through **Operations** (#/operations). That page summarizes current health, pipeline counts and failure rates, average/max latency, and terminal answer outcomes, with manual refresh. It deliberately provides no history or alerts. Non-administrators do not see the navigation item, and the backend still enforces the administrator role on every metric request.

To inspect the raw measurements from PowerShell, authenticate with the configured synthetic administrator:

```powershell
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$csrf = Invoke-RestMethod http://localhost:8080/api/session -WebSession $session
$headers = @{ $csrf.csrfHeaderName = $csrf.csrfToken }
$username = if ($env:QIP_ADMIN_USERNAME) { $env:QIP_ADMIN_USERNAME } else { "qip-admin" }
$password = if ($env:QIP_ADMIN_PASSWORD) { $env:QIP_ADMIN_PASSWORD } else { "qip-admin-local-only" }
$credentials = @{
  username = $username
  password = $password
}
Invoke-WebRequest http://localhost:8080/api/session/login `
  -Method Post -WebSession $session -Headers $headers -Body $credentials
Invoke-RestMethod http://localhost:8080/actuator/metrics -WebSession $session
Invoke-RestMethod http://localhost:8080/actuator/metrics/qip.knowledge.ingestion -WebSession $session
```

## Service-level indicators

The initial indicators are diagnostic baselines, not production promises:

- ingestion success ratio: successful extraction and indexing counts divided by all outcomes for each stage;
- retrieval failure ratio and average/max duration from `qip.knowledge.retrieval`;
- model failure ratio and average/max latency from `qip.investigations.model`;
- answer-quality outcome mix from `qip.investigations.answers`, especially the technical-failure share.

Establish representative local and hosted baselines before choosing alert thresholds. Ollama model latency depends heavily on model size and hardware, while insufficient-evidence answers intentionally record no model call.

## Bounded troubleshooting workflow

1. Copy the response's `X-Correlation-ID` and find matching JSON log events.
2. Check `/actuator/health/readiness`; details remain hidden by design.
3. Check the relevant QIP metric by stage and outcome to determine whether the issue is extraction, indexing, retrieval, or model generation.
4. Inspect the document's safe ingestion status or the question's public answer status and controlled failure reason.
5. For Ollama failures, verify the process is running and the configured model tags exist. Do not paste document bodies, prompts, model responses, credentials, or environment dumps into tickets.
6. Retry only through QIP's existing idempotent extraction/indexing operations. A question can be asked again after the provider is healthy.

Add OpenTelemetry or an external metrics/log stack only when hosted operation, history, alerting, or cross-process tracing creates a measured need.
