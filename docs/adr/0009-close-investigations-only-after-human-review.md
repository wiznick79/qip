# ADR 0009: Close investigations only after human review

## Status

Accepted on 2026-08-21.

## Context

Human-reviewed findings provide accountable conclusions, but an open investigation still needs an explicit terminal action. Closing a case without confirmed findings would allow an unsupported or entirely rejected analysis to appear complete. Continuing to modify a closed case would silently change the record that the closer approved.

## Decision

Investigations have `OPEN` and `CLOSED` states. Closure requires all draft findings to be resolved and at least one finding to be `CONFIRMED`. The closer supplies a bounded human-authored summary and caller-supplied provenance reference. Closure time is assigned by the application.

Closure is terminal and immutable. Closed investigations reject new questions, finding proposals, finding reviews, and repeated closure. The response retains the closure summary, closer reference, and timestamp alongside the question and finding history.

Until authentication exists, the closer reference remains a caller-supplied provenance label rather than a verified identity.

## Alternatives considered

### Derive closure automatically from a confirmed finding

Rejected because confirmation of an individual finding does not express the investigator's final case-level summary or remaining uncertainty.

### Allow closure with no confirmed findings

Rejected because a case containing only insufficient answers or rejected findings has no accountable conclusion to support closure.

### Permit reopening immediately

Deferred. Reopening needs an explicit reason, audit event, and interaction with the incident lifecycle. Adding it without those rules would weaken the terminal record.

## Consequences

- Closure readiness is deterministic and enforced by the backend.
- The UI hides mutation controls after closure and displays the immutable closure record.
- A draft must be confirmed or rejected before closure.
- A later reopening workflow requires a separate domain decision and audit design.

## Reevaluation triggers

Revisit this decision when authenticated identity is introduced, multiple approvers are required, investigations can be reopened, or incident and investigation status transitions must be coordinated automatically.
