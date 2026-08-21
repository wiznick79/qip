import { useEffect, useMemo, useState, type FormEvent } from "react";
import { api } from "../api";
import { EmptyState, ErrorNotice, LoadingRows } from "../components/Feedback";
import type { FindingStatus, Incident, Investigation, QuestionAnswer, SourceDocument } from "../types";
import { PageHeading } from "./AssetsPage";

export function InvestigationsPage() {
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [documents, setDocuments] = useState<SourceDocument[]>([]);
  const [selectedIncidentId, setSelectedIncidentId] = useState("");
  const [selectedDocuments, setSelectedDocuments] = useState<Set<string>>(new Set());
  const [investigation, setInvestigation] = useState<Investigation | null>(null);
  const [draftQuestionId, setDraftQuestionId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [opening, setOpening] = useState(false);
  const [asking, setAsking] = useState(false);
  const [findingAction, setFindingAction] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const indexedDocuments = useMemo(() => documents.filter((document) => document.status === "INDEXED"), [documents]);
  const selectedIncident = incidents.find((incident) => incident.id === selectedIncidentId);

  useEffect(() => {
    let active = true;
    Promise.all([api.listIncidents(), api.listDocuments()])
      .then(([incidentPage, documentPage]) => {
        if (!active) return;
        setIncidents(incidentPage.items);
        setDocuments(documentPage.items);
      })
      .catch((cause) => active && setError(message(cause)))
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, []);

  async function refresh(investigationId: string) {
    setInvestigation(await api.getInvestigation(investigationId));
  }

  async function openInvestigation() {
    if (!selectedIncidentId) return;
    setOpening(true);
    setError(null);
    try { setInvestigation(await api.createInvestigation(selectedIncidentId)); }
    catch (cause) { setError(message(cause)); }
    finally { setOpening(false); }
  }

  async function ask(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!investigation) return;
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    setAsking(true);
    setError(null);
    try {
      await api.askQuestion(investigation.id, String(form.get("question")), [...selectedDocuments]);
      formElement.reset();
      await refresh(investigation.id);
    } catch (cause) { setError(message(cause)); }
    finally { setAsking(false); }
  }

  async function proposeFinding(event: FormEvent<HTMLFormElement>, questionId: string) {
    event.preventDefault();
    if (!investigation) return;
    const form = new FormData(event.currentTarget);
    setFindingAction(questionId);
    setError(null);
    try {
      await api.proposeFinding(investigation.id, {
        sourceQuestionId: questionId,
        summary: String(form.get("summary")),
        proposedBy: String(form.get("proposedBy")),
      });
      setDraftQuestionId(null);
      await refresh(investigation.id);
    } catch (cause) { setError(message(cause)); }
    finally { setFindingAction(null); }
  }

  async function reviewFinding(event: FormEvent<HTMLFormElement>, findingId: string) {
    event.preventDefault();
    if (!investigation) return;
    const form = new FormData(event.currentTarget);
    setFindingAction(findingId);
    setError(null);
    try {
      await api.reviewFinding(investigation.id, findingId, {
        decision: String(form.get("decision")) as Exclude<FindingStatus, "DRAFT">,
        reviewerReference: String(form.get("reviewerReference")),
        rationale: String(form.get("rationale")),
      });
      await refresh(investigation.id);
    } catch (cause) { setError(message(cause)); }
    finally { setFindingAction(null); }
  }

  async function closeInvestigation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!investigation) return;
    const form = new FormData(event.currentTarget);
    setFindingAction("closure");
    setError(null);
    try {
      setInvestigation(await api.closeInvestigation(investigation.id, {
        summary: String(form.get("summary")),
        closedBy: String(form.get("closedBy")),
      }));
    } catch (cause) { setError(message(cause)); }
    finally { setFindingAction(null); }
  }

  function toggleDocument(documentId: string) {
    setSelectedDocuments((current) => {
      const next = new Set(current);
      if (next.has(documentId)) next.delete(documentId); else next.add(documentId);
      return next;
    });
  }

  return <section className="page investigation-page" aria-labelledby="investigate-title">
    <PageHeading
      eyebrow="Evidence workspace"
      title="Investigate"
      detail="Ask bounded questions, then explicitly review source-backed findings. AI answers never confirm themselves."
      count={(investigation?.questions.length ?? 0) + (investigation?.findings.length ?? 0)}
    />
    <ErrorNotice message={error} />
    {loading ? <section className="panel"><LoadingRows columns={3} /></section> : <div className="investigation-layout">
      <aside className="panel investigation-scope">
        <div className="panel-heading"><span className="step">04</span><div><h2>Case scope</h2><p>Choose an incident and optional documents.</p></div></div>
        <div className="scope-fields">
          <label>Incident<select value={selectedIncidentId} onChange={(event) => { setSelectedIncidentId(event.target.value); setInvestigation(null); setDraftQuestionId(null); }}><option value="">Select an incident</option>{incidents.map((incident) => <option value={incident.id} key={incident.id}>{incident.title}</option>)}</select></label>
          {selectedIncident ? <div className="incident-context"><strong>{selectedIncident.title}</strong><span>{selectedIncident.severity} · {selectedIncident.status}</span><p>{selectedIncident.description || "No description provided."}</p></div> : null}
          <button className="primary-button" disabled={!selectedIncidentId || opening} onClick={openInvestigation}>{opening ? "Opening…" : investigation ? "Workspace open" : "Open investigation"}</button>
          <fieldset><legend>Document scope <span>Optional</span></legend><p>With none selected, all indexed documents are searched.</p>{indexedDocuments.length === 0 ? <small>No indexed documents available.</small> : indexedDocuments.map((document) => <label className="document-choice" key={document.id}><input type="checkbox" checked={selectedDocuments.has(document.id)} onChange={() => toggleDocument(document.id)} /><span><strong>{document.title}</strong><small>{document.extractedPageCount} page{document.extractedPageCount === 1 ? "" : "s"}</small></span></label>)}</fieldset>
        </div>
      </aside>
      <section className="panel investigation-thread">
        <div className="panel-heading"><div><h2>Grounded questions</h2><p>Answers are decision support, not confirmed findings.</p></div></div>
        {!investigation ? <EmptyState title="No investigation open" detail="Select an incident to begin or resume its investigation." /> : <>
          <div className="answer-history">
            {investigation.questions.length === 0
              ? <EmptyState title="No questions yet" detail="Ask a focused question about the indexed technical evidence." />
              : investigation.questions.map((item) => <AnswerCard
                key={item.id}
                item={item}
                findingExists={investigation.findings.some((finding) => finding.sourceQuestionId === item.id)}
                canPropose={investigation.status === "OPEN"}
                drafting={draftQuestionId === item.id}
                busy={findingAction === item.id}
                onToggleDraft={() => setDraftQuestionId((current) => current === item.id ? null : item.id)}
                onPropose={(event) => proposeFinding(event, item.id)}
              />)}
          </div>
          <FindingsPanel investigation={investigation} busyAction={findingAction} onReview={reviewFinding} onClose={closeInvestigation} />
          {investigation.status === "OPEN" ? <form className="question-form" onSubmit={ask}><label>Question<textarea name="question" required maxLength={1000} rows={3} placeholder="What evidence supports inspecting the hydraulic seal?" /></label><button className="primary-button" disabled={asking || indexedDocuments.length === 0}>{asking ? "Reviewing evidence…" : "Ask with evidence"}</button></form> : null}
        </>}
      </section>
    </div>}
  </section>;
}

function AnswerCard({
  item,
  findingExists,
  canPropose,
  drafting,
  busy,
  onToggleDraft,
  onPropose,
}: {
  item: QuestionAnswer;
  findingExists: boolean;
  canPropose: boolean;
  drafting: boolean;
  busy: boolean;
  onToggleDraft: () => void;
  onPropose: (event: FormEvent<HTMLFormElement>) => void;
}) {
  return <article className="answer-card">
    <p className="question-text">{item.question}</p>
    <span className={`answer-status answer-status-${item.status.toLowerCase().replaceAll("_", "-")}`}>{item.status.replaceAll("_", " ")}</span>
    {item.answer ? <p className="answer-text">{item.answer}</p> : <p className="answer-failure">{item.failureReason}</p>}
    {item.citations.length > 0 ? <div className="citations"><strong>Sources</strong>{item.citations.map((citation) => <details key={citation.passageId}><summary>{citation.documentTitle} · page {citation.pageNumber}</summary><p>{citation.excerpt}</p><small>Passage {citation.passageSequence + 1} · relevance {citation.relevanceScore.toFixed(3)}</small></details>)}</div> : null}
    {canPropose && item.status === "GROUNDED" && !findingExists ? <button className="quiet-button finding-toggle" onClick={onToggleDraft}>{drafting ? "Cancel draft" : "Propose as finding"}</button> : null}
    {findingExists ? <p className="finding-linked">A reviewable finding has been created from this answer.</p> : null}
    {drafting ? <form className="finding-draft-form" onSubmit={onPropose}>
      <label>Finding summary<textarea name="summary" required maxLength={2000} rows={3} defaultValue={item.answer ?? ""} /></label>
      <label>Proposed by<input name="proposedBy" required maxLength={120} defaultValue="wiznick79" /><span>Local provenance label; authentication is not enabled yet.</span></label>
      <button className="primary-button" disabled={busy}>{busy ? "Creating draft…" : "Create draft finding"}</button>
    </form> : null}
    <footer>{item.modelId ?? "No model used"} · {item.promptVersion}</footer>
  </article>;
}

function FindingsPanel({
  investigation,
  busyAction,
  onReview,
  onClose,
}: {
  investigation: Investigation;
  busyAction: string | null;
  onReview: (event: FormEvent<HTMLFormElement>, findingId: string) => void;
  onClose: (event: FormEvent<HTMLFormElement>) => void;
}) {
  const hasConfirmedFinding = investigation.findings.some((finding) => finding.status === "CONFIRMED");
  const hasDraftFinding = investigation.findings.some((finding) => finding.status === "DRAFT");
  return <section className="findings-panel">
    <div className="findings-heading"><div><h3>Human-reviewed findings</h3><p>Only an explicit review can confirm or reject a draft.</p></div><span>{investigation.findings.length}</span></div>
    {investigation.findings.length === 0 ? <p className="findings-empty">No findings proposed yet.</p> : investigation.findings.map((finding) => <article className="finding-card" key={finding.id}>
      <span className={`finding-status finding-status-${finding.status.toLowerCase()}`}>{finding.status}</span>
      <p>{finding.summary}</p>
      <small>Proposed by {finding.proposedBy} · {formatDate(finding.proposedAt)}</small>
      {finding.reviewedBy ? <div className="review-outcome"><strong>{finding.status} by {finding.reviewedBy}</strong><p>{finding.reviewRationale}</p></div> : null}
      <details className="review-history"><summary>Audit history · {finding.events.length} event{finding.events.length === 1 ? "" : "s"}</summary>{finding.events.map((event) => <p key={event.id}><strong>{event.type}</strong> · {event.actorReference} · {formatDate(event.occurredAt)}{event.rationale ? ` — ${event.rationale}` : ""}</p>)}</details>
      {finding.status === "DRAFT" ? <form className="review-form" onSubmit={(event) => onReview(event, finding.id)}>
        <div className="field-pair"><label>Decision<select name="decision" defaultValue="CONFIRMED"><option value="CONFIRMED">Confirm</option><option value="REJECTED">Reject</option></select></label><label>Reviewer<input name="reviewerReference" required maxLength={120} defaultValue="wiznick79" /></label></div>
        <label>Rationale<textarea name="rationale" required maxLength={1000} rows={2} placeholder="Explain the evidence-based review decision." /></label>
        <button className="primary-button" disabled={busyAction === finding.id}>{busyAction === finding.id ? "Recording review…" : "Record review decision"}</button>
      </form> : null}
    </article>)}
    {investigation.status === "CLOSED" ? <div className="closure-outcome"><span>CLOSED</span><h3>Case closure</h3><p>{investigation.closureSummary}</p><small>Closed by {investigation.closedBy} · {formatDate(investigation.closedAt!)}</small></div>
      : hasConfirmedFinding && !hasDraftFinding ? <form className="closure-form" onSubmit={onClose}>
        <div><h3>Close investigation</h3><p>Closure is terminal. Summarize the human-reviewed conclusion without overstating the evidence.</p></div>
        <label>Closure summary<textarea name="summary" required maxLength={4000} rows={3} placeholder="Summarize the confirmed findings, uncertainty, and any follow-up." /></label>
        <label>Closed by<input name="closedBy" required maxLength={120} defaultValue="wiznick79" /><span>Local provenance label; authentication is not enabled yet.</span></label>
        <button className="primary-button" disabled={busyAction === "closure"}>{busyAction === "closure" ? "Closing investigation…" : "Close investigation"}</button>
      </form>
      : <p className="closure-readiness">Resolve every draft and confirm at least one finding before closing the investigation.</p>}
  </section>;
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}

function message(cause: unknown) { return cause instanceof Error ? cause.message : "Something went wrong."; }
