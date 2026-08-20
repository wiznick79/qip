# ADR 0008: Require explicit human review for investigation findings

## Status

Accepted on 2026-08-20.

## Context

Grounded answers preserve citation provenance, but they are still generated decision support. Treating an answer as a confirmed finding would erase the distinction between model output and accountable human judgment. QIP also needs a review history that cannot be silently overwritten.

## Decision

A user may explicitly propose one draft finding from a `GROUNDED` question that has validated citations. The submitted summary and caller-supplied proposer reference are stored separately from the generated answer. `INSUFFICIENT_EVIDENCE`, `TECHNICAL_FAILURE`, and processing questions cannot become findings.

A draft has one terminal review transition: `CONFIRMED` or `REJECTED`. The reviewer reference and rationale are mandatory. Proposal and review actions append immutable audit events; a reviewed finding cannot be reviewed again. Until authentication is implemented, actor references are provenance labels supplied by the caller and are presented as such in the UI.

## Alternatives considered

### Automatically confirm grounded answers

Rejected because citation validation proves that returned source identifiers are permitted, not that every claim is correct or that the proposed conclusion is operationally valid.

### Copy generated answers directly into incident evidence

Rejected because the existing evidence endpoint deliberately assigns human-entered provenance. A model-generated answer must not gain human-confirmed provenance without an explicit review boundary.

### Allow findings to be edited after review

Rejected for this slice because silent edits weaken the audit trail. A materially different conclusion should originate from another grounded question and review action.

## Consequences

- The UI exposes a visible proposal and review workflow rather than an automatic promotion.
- Each grounded question can source at most one finding.
- Confirmed and rejected findings are immutable.
- Authentication and verified identities remain future work; actor references must not be represented as authenticated identities.
- Investigation closure remains a subsequent Milestone 11 increment.

## Reevaluation triggers

Revisit this decision when authenticated identity is added, review requires multiple approvers, a finding must be amended rather than superseded, or regulatory retention requirements demand a stronger event model.
