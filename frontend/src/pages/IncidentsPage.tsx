import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";
import { api } from "../api";
import { EmptyState, ErrorNotice, LoadingRows } from "../components/Feedback";
import type { Asset, Incident, IncidentSeverity, IncidentStatus } from "../types";
import { PageHeading } from "./AssetsPage";

const statuses: (IncidentStatus | "")[] = ["", "REPORTED", "INVESTIGATING", "RESOLVED", "CLOSED"];

const PAGE_SIZE = 20;

export function IncidentsPage({
  onInvestigate,
  onOpenCase,
}: {
  onInvestigate?: (incidentId: string) => void;
  onOpenCase?: (incidentId: string) => void;
}) {
  const [assets, setAssets] = useState<Asset[]>([]);
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [status, setStatus] = useState<IncidentStatus | "">("");
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [changingIncidentId, setChangingIncidentId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const assetNames = useMemo(() => new Map(assets.map((asset) => [asset.id, asset.name])), [assets]);

  const load = useCallback(async () => {
    setLoading(true); setError(null);
    try {
      const [assetPage, incidentPage] = await Promise.all([
        api.listAssets(),
        api.listIncidents(status || undefined, page, PAGE_SIZE),
      ]);
      setAssets(assetPage.items); setIncidents(incidentPage.items); setTotalElements(incidentPage.totalElements);
    } catch (cause) { setError(message(cause)); } finally { setLoading(false); }
  }, [page, status]);

  useEffect(() => void load(), [load]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const formElement = event.currentTarget; const form = new FormData(formElement);
    setSaving(true); setError(null);
    try {
      await api.createIncident({
        assetId: String(form.get("assetId")), title: String(form.get("title")),
        description: String(form.get("description") || "") || null,
        severity: String(form.get("severity")) as IncidentSeverity,
        occurredAt: new Date(String(form.get("occurredAt"))).toISOString(),
      });
      formElement.reset();
      if (page === 0) await load(); else setPage(0);
    } catch (cause) { setError(message(cause)); } finally { setSaving(false); }
  }

  async function transition(incident: Incident, requestedStatus: IncidentStatus, openWorkspace = false) {
    setChangingIncidentId(incident.id); setError(null);
    try {
      await api.updateIncidentStatus(incident.id, requestedStatus);
      if (openWorkspace) onInvestigate?.(incident.id);
      else if (page === 0) await load();
      else setPage(0);
    } catch (cause) { setError(message(cause)); } finally { setChangingIncidentId(null); }
  }

  function workspaceAction(incident: Incident) {
    if (incident.status === "REPORTED") {
      return <button className="quiet-button" disabled={changingIncidentId === incident.id} onClick={() => transition(incident, "INVESTIGATING", true)}>Start investigation</button>;
    }
    return <button className="quiet-button" onClick={() => onInvestigate?.(incident.id)}>{incident.status === "INVESTIGATING" ? "Open workspace" : "View history"}</button>;
  }

  const pageCount = Math.max(1, Math.ceil(totalElements / PAGE_SIZE));

  return <section className="page" aria-labelledby="incidents-title">
    <PageHeading eyebrow="Case intake" title="Incidents" detail="Capture abnormal conditions and keep their investigation state visible." count={totalElements} />
    <ErrorNotice message={error} />
    <div className="split-layout">
      <article className="panel form-panel">
        <div className="panel-heading"><span className="step">02</span><div><h2>Report an incident</h2><p>Link the event to a registered asset.</p></div></div>
        {assets.length === 0 && !loading ? <p className="inline-hint">Register an asset before reporting an incident.</p> : null}
        <form onSubmit={submit}>
          <label>Asset<select name="assetId" required defaultValue=""><option value="" disabled>Select an asset</option>{assets.map((asset) => <option key={asset.id} value={asset.id}>{asset.name}</option>)}</select></label>
          <label>Title<input name="title" required maxLength={160} placeholder="e.g. Unexpected spindle vibration" /></label>
          <div className="field-pair"><label>Severity<select name="severity" defaultValue="MEDIUM"><option>LOW</option><option>MEDIUM</option><option>HIGH</option><option>CRITICAL</option></select></label><label>Occurred at<input name="occurredAt" type="datetime-local" required /></label></div>
          <label>Description <span className="optional">Optional</span><textarea name="description" maxLength={4000} rows={4} placeholder="Describe what was observed, without assigning a cause." /></label>
          <button className="primary-button" disabled={saving || assets.length === 0}>{saving ? "Reporting…" : "Report incident"}</button>
        </form>
      </article>
      <article className="panel list-panel">
        <div className="panel-heading filter-heading"><div><h2>Incident queue</h2><p>Newest occurrence first.</p></div><label className="compact-label">Status<select aria-label="Filter incidents by status" value={status} onChange={(event) => { setStatus(event.target.value as IncidentStatus | ""); setPage(0); }}>{statuses.map((item) => <option key={item || "ALL"} value={item}>{item || "ALL"}</option>)}</select></label></div>
        {loading ? <LoadingRows columns={4} /> : incidents.length === 0 ? <EmptyState title="No matching incidents" detail="Report an incident or change the status filter." /> : <div className="records">{incidents.map((incident) => <div className="record incident-record" key={incident.id}><span className={`severity severity-${incident.severity.toLowerCase()}`}>{incident.severity.slice(0, 1)}</span><div className="record-main"><strong>{incident.title}</strong><span>{assetNames.get(incident.assetId) ?? "Unknown asset"} · {formatDate(incident.occurredAt)}</span></div><span className={`status status-${incident.status.toLowerCase()}`}>{incident.status}</span><div className="incident-actions"><button className="quiet-button" onClick={() => onOpenCase?.(incident.id)}>Case details</button>{workspaceAction(incident)}{incident.status === "INVESTIGATING" ? <button className="quiet-button" disabled={changingIncidentId === incident.id} onClick={() => transition(incident, "RESOLVED")}>Mark resolved</button> : null}{incident.status === "RESOLVED" ? <><button className="quiet-button" disabled={changingIncidentId === incident.id} onClick={() => transition(incident, "INVESTIGATING")}>Reopen</button><button className="quiet-button" disabled={changingIncidentId === incident.id} onClick={() => transition(incident, "CLOSED")}>Close incident</button></> : null}</div></div>)}</div>}
        <div className="pagination"><button className="quiet-button" disabled={loading || page === 0} onClick={() => setPage((current) => current - 1)}>Previous</button><span>Page {page + 1} of {pageCount} · {totalElements} incident{totalElements === 1 ? "" : "s"}</span><button className="quiet-button" disabled={loading || page + 1 >= pageCount} onClick={() => setPage((current) => current + 1)}>Next</button></div>
      </article>
    </div>
  </section>;
}

function formatDate(value: string) { return new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)); }
function message(cause: unknown) { return cause instanceof Error ? cause.message : "Something went wrong."; }
