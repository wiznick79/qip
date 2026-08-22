import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";
import { api } from "../api";
import { EmptyState, ErrorNotice, LoadingRows } from "../components/Feedback";
import type { Asset, EvidenceItem, EvidenceType, Incident, Observation, Page } from "../types";
import { PageHeading } from "./AssetsPage";

const PAGE_SIZE = 10;
const evidenceTypes: EvidenceType[] = ["MEASUREMENT", "DOCUMENT", "IMAGE", "LOG_ENTRY", "PHYSICAL_ITEM", "TEST_RESULT", "OTHER"];

export function IncidentWorkspacePage({
  incidentId,
  onBack,
  onInvestigate,
}: {
  incidentId: string;
  onBack?: () => void;
  onInvestigate?: (incidentId: string) => void;
}) {
  const [incident, setIncident] = useState<Incident | null>(null);
  const [assets, setAssets] = useState<Asset[]>([]);
  const [observations, setObservations] = useState<Page<Observation> | null>(null);
  const [evidence, setEvidence] = useState<Page<EvidenceItem> | null>(null);
  const [observationPage, setObservationPage] = useState(0);
  const [evidencePage, setEvidencePage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState<"observation" | "evidence" | "investigation" | null>(null);
  const [error, setError] = useState<string | null>(null);
  const assetName = useMemo(() => assets.find((asset) => asset.id === incident?.assetId)?.name ?? "Unknown asset", [assets, incident]);

  useEffect(() => {
    let active = true;
    setLoading(true); setError(null);
    Promise.all([api.getIncident(incidentId), api.listAssets()])
      .then(([loadedIncident, assetPage]) => { if (active) { setIncident(loadedIncident); setAssets(assetPage.items); } })
      .catch((cause) => active && setError(message(cause)))
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, [incidentId]);

  const loadObservations = useCallback(async () => {
    try { setObservations(await api.listObservations(incidentId, observationPage, PAGE_SIZE)); }
    catch (cause) { setError(message(cause)); }
  }, [incidentId, observationPage]);

  const loadEvidence = useCallback(async () => {
    try { setEvidence(await api.listEvidence(incidentId, evidencePage, PAGE_SIZE)); }
    catch (cause) { setError(message(cause)); }
  }, [evidencePage, incidentId]);

  useEffect(() => void loadObservations(), [loadObservations]);
  useEffect(() => void loadEvidence(), [loadEvidence]);

  async function appendObservation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const formElement = event.currentTarget; const form = new FormData(formElement);
    setSaving("observation"); setError(null);
    try {
      await api.appendObservation(incidentId, {
        text: String(form.get("text")), authorReference: String(form.get("authorReference")),
        observedAt: new Date(String(form.get("observedAt"))).toISOString(),
      });
      formElement.reset();
      const appendedPage = Math.floor((observations?.totalElements ?? 0) / PAGE_SIZE);
      if (observationPage === appendedPage) await loadObservations(); else setObservationPage(appendedPage);
    } catch (cause) { setError(message(cause)); } finally { setSaving(null); }
  }

  async function appendEvidence(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const formElement = event.currentTarget; const form = new FormData(formElement);
    setSaving("evidence"); setError(null);
    try {
      await api.appendEvidence(incidentId, {
        type: String(form.get("type")) as EvidenceType, summary: String(form.get("summary")),
        sourceReference: String(form.get("sourceReference")),
        eventAt: new Date(String(form.get("eventAt"))).toISOString(), submittedBy: String(form.get("submittedBy")),
      });
      formElement.reset();
      const appendedPage = Math.floor((evidence?.totalElements ?? 0) / PAGE_SIZE);
      if (evidencePage === appendedPage) await loadEvidence(); else setEvidencePage(appendedPage);
    } catch (cause) { setError(message(cause)); } finally { setSaving(null); }
  }

  async function openInvestigation() {
    if (!incident) return;
    setSaving("investigation"); setError(null);
    try {
      if (incident.status === "REPORTED") setIncident(await api.updateIncidentStatus(incident.id, "INVESTIGATING"));
      onInvestigate?.(incident.id);
    } catch (cause) { setError(message(cause)); } finally { setSaving(null); }
  }

  if (loading) return <section className="page"><section className="panel"><LoadingRows columns={3} /></section></section>;

  return <section className="page case-page" aria-labelledby="case-title">
    <PageHeading headingId="case-title" eyebrow="Incident record" title={incident?.title ?? "Case unavailable"} detail="Preserve what people observed and the evidence they identified without silently rewriting history." count={(observations?.totalElements ?? 0) + (evidence?.totalElements ?? 0)} />
    <ErrorNotice message={error} />
    {!incident ? <EmptyState title="Incident unavailable" detail="Return to the incident queue and select an existing case." /> : <>
      <section className="case-summary panel">
        <div><button className="quiet-button" onClick={onBack}>← Incident queue</button><span className={`status status-${incident.status.toLowerCase()}`}>{incident.status}</span></div>
        <dl><div><dt>Asset</dt><dd>{assetName}</dd></div><div><dt>Severity</dt><dd>{incident.severity}</dd></div><div><dt>Occurred</dt><dd>{formatDate(incident.occurredAt)}</dd></div><div><dt>Incident ID</dt><dd><code>{incident.id}</code></dd></div></dl>
        <p>{incident.description || "No incident description provided."}</p>
        <button className="primary-button" disabled={saving === "investigation"} onClick={openInvestigation}>{saving === "investigation" ? "Opening…" : incident.status === "REPORTED" ? "Start investigation" : incident.status === "INVESTIGATING" ? "Open investigation" : "View investigation history"}</button>
      </section>
      <aside className="context-boundary"><strong>Human incident record</strong><p>These append-only entries preserve caller-supplied provenance. They are not authenticated identities and are not sent to the AI model in Milestone 13.</p></aside>
      <div className="case-columns">
        <TimelinePanel title="Observations" detail="Human-authored notes in observation-time order." page={observations} currentPage={observationPage} setPage={setObservationPage} render={(item) => <article className="timeline-entry" key={item.id}><span>OBSERVATION</span><p>{item.text}</p><footer>{item.authorReference} · observed {formatDate(item.observedAt)} · recorded {formatDate(item.recordedAt)}</footer></article>} />
        <TimelinePanel title="Evidence" detail="Source-attributed items considered during the investigation." page={evidence} currentPage={evidencePage} setPage={setEvidencePage} render={(item) => <article className="timeline-entry evidence-entry" key={item.id}><span>{item.type.replaceAll("_", " ")}</span><p>{item.summary}</p><strong>{item.sourceReference}</strong><footer>{item.provenance.replaceAll("_", " ")} · {item.submittedBy} · event {formatDate(item.eventAt)}</footer></article>} />
      </div>
      <div className="case-columns case-forms">
        <section className="panel"><div className="panel-heading"><span className="step">13A</span><div><h2>Add observation</h2><p>Corrections are new entries, never silent edits.</p></div></div><form onSubmit={appendObservation}><label>Observation<textarea name="text" required maxLength={4000} rows={4} placeholder="Record what was directly observed." /></label><label>Observed at<input name="observedAt" type="datetime-local" required defaultValue={localDateTimeNow()} /></label><label>Author reference<input name="authorReference" required maxLength={120} defaultValue="wiznick79" /><span className="field-note">Local provenance label; authentication is not enabled.</span></label><button className="primary-button" disabled={saving === "observation"}>{saving === "observation" ? "Recording…" : "Record observation"}</button></form></section>
        <section className="panel"><div className="panel-heading"><span className="step">13B</span><div><h2>Add evidence</h2><p>Record origin without claiming a confirmed cause.</p></div></div><form onSubmit={appendEvidence}><label>Type<select name="type" defaultValue="MEASUREMENT">{evidenceTypes.map((type) => <option key={type} value={type}>{type.replaceAll("_", " ")}</option>)}</select></label><label>Summary<textarea name="summary" required maxLength={1000} rows={3} placeholder="Describe the evidence and its relevance without overstating it." /></label><label>Source reference<input name="sourceReference" required maxLength={500} placeholder="e.g. Gauge PT-14, operator log 08:45" /></label><label>Event time<input name="eventAt" type="datetime-local" required defaultValue={localDateTimeNow()} /></label><label>Submitted by<input name="submittedBy" required maxLength={120} defaultValue="wiznick79" /><span className="field-note">The server assigns HUMAN_ENTERED provenance.</span></label><button className="primary-button" disabled={saving === "evidence"}>{saving === "evidence" ? "Recording…" : "Record evidence"}</button></form></section>
      </div>
    </>}
  </section>;
}

function TimelinePanel<T>({ title, detail, page, currentPage, setPage, render }: { title: string; detail: string; page: Page<T> | null; currentPage: number; setPage: (page: number) => void; render: (item: T) => React.ReactNode }) {
  const pageCount = Math.max(1, Math.ceil((page?.totalElements ?? 0) / PAGE_SIZE));
  return <section className="panel timeline-panel"><div className="panel-heading"><div><h2>{title}</h2><p>{detail}</p></div><span className="timeline-count">{page?.totalElements ?? 0}</span></div>{!page ? <LoadingRows columns={2} /> : page.items.length === 0 ? <EmptyState title={`No ${title.toLowerCase()} yet`} detail="Use the form below to append the first entry." /> : <div className="timeline-list">{page.items.map(render)}</div>}<div className="pagination"><button className="quiet-button" disabled={currentPage === 0} onClick={() => setPage(currentPage - 1)}>Previous</button><span>Page {currentPage + 1} of {pageCount}</span><button className="quiet-button" disabled={currentPage + 1 >= pageCount} onClick={() => setPage(currentPage + 1)}>Next</button></div></section>;
}

function localDateTimeNow() { const now = new Date(); return new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 16); }
function formatDate(value: string) { return new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)); }
function message(cause: unknown) { return cause instanceof Error ? cause.message : "Something went wrong."; }
