import { useEffect, useMemo, useState } from "react";
import { api } from "../api";
import { EmptyState, ErrorNotice, LoadingRows } from "../components/Feedback";
import type { Asset, Incident } from "../types";

type DashboardData = {
  assets: Asset[];
  assetCount: number;
  incidents: Incident[];
  incidentCount: number;
  documentCount: number;
  indexedDocumentCount: number;
};

const emptyData: DashboardData = {
  assets: [],
  assetCount: 0,
  incidents: [],
  incidentCount: 0,
  documentCount: 0,
  indexedDocumentCount: 0,
};

export function DashboardPage({
  onNavigate,
  onOpenIncident,
}: {
  onNavigate: (view: "assets" | "incidents" | "documents" | "investigations") => void;
  onOpenIncident: (incidentId: string) => void;
}) {
  const [data, setData] = useState<DashboardData>(emptyData);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const assetNames = useMemo(() => new Map(data.assets.map((asset) => [asset.id, asset.name])), [data.assets]);

  useEffect(() => {
    let active = true;
    Promise.all([
      api.listAssets(),
      api.listIncidents(undefined, 0, 5),
      api.listDocuments(),
    ]).then(([assets, incidents, documents]) => {
      if (!active) return;
      setData({
        assets: assets.items,
        assetCount: assets.totalElements,
        incidents: incidents.items,
        incidentCount: incidents.totalElements,
        documentCount: documents.totalElements,
        indexedDocumentCount: documents.items.filter((document) => document.status === "INDEXED").length,
      });
    }).catch((cause) => active && setError(message(cause)))
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, []);

  return <section className="page dashboard-page" aria-labelledby="dashboard-title">
    <header className="page-heading dashboard-heading"><div><p className="eyebrow">Workspace overview</p><h1 id="dashboard-title">Dashboard</h1><p>A quick view of current cases and available sources.</p></div></header>
    <ErrorNotice message={error} />
    <div className="dashboard-metrics">
      <Summary label="Assets" value={data.assetCount} />
      <Summary label="Incidents" value={data.incidentCount} />
      <Summary label="Documents" value={data.documentCount} />
      <Summary label="Indexed" value={data.indexedDocumentCount} />
    </div>
    <div className="dashboard-grid">
      <article className="panel list-panel">
        <div className="panel-heading"><div><h2>Recent incidents</h2><p>Newest occurrences first.</p></div><button className="quiet-button" onClick={() => onNavigate("incidents")}>View all</button></div>
        {loading ? <LoadingRows columns={3} /> : data.incidents.length === 0
          ? <EmptyState title="No incidents yet" detail="Report an incident to begin tracking a case." />
          : <div className="records">{data.incidents.map((incident) => <div className="record dashboard-incident" key={incident.id}>
            <span className={`severity severity-${incident.severity.toLowerCase()}`}>{incident.severity.slice(0, 1)}</span>
            <div className="record-main"><strong>{incident.title}</strong><span>{assetNames.get(incident.assetId) ?? "Unknown asset"} · {formatDate(incident.occurredAt)}</span></div>
            <span className={`status status-${incident.status.toLowerCase()}`}>{incident.status}</span>
            <button className="quiet-button" onClick={() => onOpenIncident(incident.id)}>Open</button>
          </div>)}</div>}
      </article>
      <article className="panel dashboard-actions">
        <div className="panel-heading"><div><h2>Quick actions</h2><p>Continue the investigation workflow.</p></div></div>
        <div>
          <button onClick={() => onNavigate("assets")}><span>01</span><strong>Register an asset</strong><small>Add equipment or a production line.</small></button>
          <button onClick={() => onNavigate("incidents")}><span>02</span><strong>Report an incident</strong><small>Record a new abnormal condition.</small></button>
          <button onClick={() => onNavigate("documents")}><span>03</span><strong>Upload a document</strong><small>Add a technical source for retrieval.</small></button>
          <button onClick={() => onNavigate("investigations")}><span>04</span><strong>Open investigations</strong><small>Review questions, findings, and case status.</small></button>
        </div>
      </article>
    </div>
  </section>;
}

function Summary({ label, value }: { label: string; value: number }) {
  return <article className="panel dashboard-summary"><span>{label}</span><strong>{value}</strong></article>;
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}

function message(cause: unknown) {
  return cause instanceof Error ? cause.message : "The dashboard could not be loaded.";
}
