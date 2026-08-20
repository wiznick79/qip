import { useEffect, useMemo, useState, type FormEvent } from "react";
import { api } from "../api";
import { EmptyState, ErrorNotice, LoadingRows } from "../components/Feedback";
import type { Incident, Investigation, SourceDocument } from "../types";
import { PageHeading } from "./AssetsPage";

export function InvestigationsPage() {
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [documents, setDocuments] = useState<SourceDocument[]>([]);
  const [selectedIncidentId, setSelectedIncidentId] = useState("");
  const [selectedDocuments, setSelectedDocuments] = useState<Set<string>>(new Set());
  const [investigation, setInvestigation] = useState<Investigation | null>(null);
  const [loading, setLoading] = useState(true);
  const [opening, setOpening] = useState(false);
  const [asking, setAsking] = useState(false);
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

  async function openInvestigation() {
    if (!selectedIncidentId) return;
    setOpening(true); setError(null);
    try { setInvestigation(await api.createInvestigation(selectedIncidentId)); }
    catch (cause) { setError(message(cause)); }
    finally { setOpening(false); }
  }

  async function ask(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!investigation) return;
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    setAsking(true); setError(null);
    try {
      await api.askQuestion(investigation.id, String(form.get("question")), [...selectedDocuments]);
      formElement.reset();
      setInvestigation(await api.getInvestigation(investigation.id));
    } catch (cause) { setError(message(cause)); }
    finally { setAsking(false); }
  }

  function toggleDocument(documentId: string) {
    setSelectedDocuments((current) => {
      const next = new Set(current);
      if (next.has(documentId)) next.delete(documentId); else next.add(documentId);
      return next;
    });
  }

  return <section className="page investigation-page" aria-labelledby="investigate-title">
    <PageHeading eyebrow="Evidence workspace" title="Investigate" detail="Ask bounded questions against indexed sources. Every supported answer retains exact passage citations." count={investigation?.questions.length ?? 0} />
    <ErrorNotice message={error} />
    {loading ? <section className="panel"><LoadingRows columns={3} /></section> : <div className="investigation-layout">
      <aside className="panel investigation-scope">
        <div className="panel-heading"><span className="step">04</span><div><h2>Case scope</h2><p>Choose an incident and optional documents.</p></div></div>
        <div className="scope-fields">
          <label>Incident<select value={selectedIncidentId} onChange={(event) => { setSelectedIncidentId(event.target.value); setInvestigation(null); }}><option value="">Select an incident</option>{incidents.map((incident) => <option value={incident.id} key={incident.id}>{incident.title}</option>)}</select></label>
          {selectedIncident ? <div className="incident-context"><strong>{selectedIncident.title}</strong><span>{selectedIncident.severity} · {selectedIncident.status}</span><p>{selectedIncident.description || "No description provided."}</p></div> : null}
          <button className="primary-button" disabled={!selectedIncidentId || opening} onClick={openInvestigation}>{opening ? "Opening…" : investigation ? "Workspace open" : "Open investigation"}</button>
          <fieldset><legend>Document scope <span>Optional</span></legend><p>With none selected, all indexed documents are searched.</p>{indexedDocuments.length === 0 ? <small>No indexed documents available.</small> : indexedDocuments.map((document) => <label className="document-choice" key={document.id}><input type="checkbox" checked={selectedDocuments.has(document.id)} onChange={() => toggleDocument(document.id)} /><span><strong>{document.title}</strong><small>{document.extractedPageCount} page{document.extractedPageCount === 1 ? "" : "s"}</small></span></label>)}</fieldset>
        </div>
      </aside>
      <section className="panel investigation-thread">
        <div className="panel-heading"><div><h2>Grounded questions</h2><p>Answers are decision support, not confirmed findings.</p></div></div>
        {!investigation ? <EmptyState title="No investigation open" detail="Select an incident to begin or resume its investigation." /> : <>
          <div className="answer-history">{investigation.questions.length === 0 ? <EmptyState title="No questions yet" detail="Ask a focused question about the indexed technical evidence." /> : investigation.questions.map((item) => <article className="answer-card" key={item.id}><p className="question-text">{item.question}</p><span className={`answer-status answer-status-${item.status.toLowerCase().replaceAll("_", "-")}`}>{item.status.replaceAll("_", " ")}</span>{item.answer ? <p className="answer-text">{item.answer}</p> : <p className="answer-failure">{item.failureReason}</p>}{item.citations.length > 0 ? <div className="citations"><strong>Sources</strong>{item.citations.map((citation) => <details key={citation.passageId}><summary>{citation.documentTitle} · page {citation.pageNumber}</summary><p>{citation.excerpt}</p><small>Passage {citation.passageSequence + 1} · relevance {citation.relevanceScore.toFixed(3)}</small></details>)}</div> : null}<footer>{item.modelId ?? "No model used"} · {item.promptVersion}</footer></article>)}</div>
          <form className="question-form" onSubmit={ask}><label>Question<textarea name="question" required maxLength={1000} rows={3} placeholder="What evidence supports inspecting the hydraulic seal?" /></label><button className="primary-button" disabled={asking || indexedDocuments.length === 0}>{asking ? "Reviewing evidence…" : "Ask with evidence"}</button></form>
        </>}
      </section>
    </div>}
  </section>;
}

function message(cause: unknown) { return cause instanceof Error ? cause.message : "Something went wrong."; }
