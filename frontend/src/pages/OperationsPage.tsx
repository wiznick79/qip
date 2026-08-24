import { useCallback, useEffect, useState } from "react";
import { api } from "../api";
import { ErrorNotice, LoadingRows } from "../components/Feedback";
import type { MetricSnapshot } from "../types";

type PipelineMetric = { total: number; failures: number; averageMs: number; maxMs: number };
type OperationsData = {
  health: string;
  extraction: PipelineMetric;
  indexing: PipelineMetric;
  retrieval: PipelineMetric;
  model: PipelineMetric;
  grounded: number;
  insufficient: number;
  technicalFailures: number;
};

export function OperationsPage() {
  const [data, setData] = useState<OperationsData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [
        health,
        extraction,
        extractionFailures,
        indexing,
        indexingFailures,
        retrieval,
        retrievalFailures,
        model,
        modelFailures,
        grounded,
        insufficient,
        technicalFailures,
      ] = await Promise.all([
        api.getHealth(),
        api.getMetric("qip.knowledge.ingestion", { stage: "extraction" }),
        api.getMetric("qip.knowledge.ingestion", { stage: "extraction", outcome: "failure" }),
        api.getMetric("qip.knowledge.ingestion", { stage: "indexing" }),
        api.getMetric("qip.knowledge.ingestion", { stage: "indexing", outcome: "failure" }),
        api.getMetric("qip.knowledge.retrieval"),
        api.getMetric("qip.knowledge.retrieval", { outcome: "failure" }),
        api.getMetric("qip.investigations.model"),
        api.getMetric("qip.investigations.model", { outcome: "failure" }),
        api.getMetric("qip.investigations.answers", { status: "GROUNDED" }),
        api.getMetric("qip.investigations.answers", { status: "INSUFFICIENT_EVIDENCE" }),
        api.getMetric("qip.investigations.answers", { status: "TECHNICAL_FAILURE" }),
      ]);
      setData({
        health: health.status,
        extraction: pipeline(extraction, extractionFailures),
        indexing: pipeline(indexing, indexingFailures),
        retrieval: pipeline(retrieval, retrievalFailures),
        model: pipeline(model, modelFailures),
        grounded: measurement(grounded, "COUNT"),
        insufficient: measurement(insufficient, "COUNT"),
        technicalFailures: measurement(technicalFailures, "COUNT"),
      });
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Operational metrics could not be loaded.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  return <section className="page operations-page" aria-labelledby="operations-title">
    <header className="page-heading operations-heading">
      <div><p className="eyebrow">Current process</p><h1 id="operations-title">Operations</h1><p>Health and bounded diagnostics since QIP last started.</p></div>
      <button className="quiet-button" disabled={loading} onClick={() => void load()}>{loading ? "Refreshing…" : "Refresh"}</button>
    </header>
    <ErrorNotice message={error} />
    {loading && !data ? <section className="panel"><LoadingRows columns={4} /></section> : data ? <>
      <div className="operations-summary">
        <Summary label="Service health" value={data.health} state={data.health === "UP" ? "healthy" : "warning"} />
        <Summary label="Ingestion failures" value={data.extraction.failures + data.indexing.failures} />
        <Summary label="Model failures" value={data.model.failures} />
        <Summary label="Grounded answers" value={data.grounded} />
      </div>
      <div className="operations-grid">
        <article className="panel operations-pipeline">
          <div className="panel-heading"><div><h2>Pipeline activity</h2><p>Counts and latency for this application process.</p></div></div>
          <div className="operations-table-wrap"><table><thead><tr><th>Stage</th><th>Runs</th><th>Failures</th><th>Failure rate</th><th>Average</th><th>Maximum</th></tr></thead><tbody>
            <PipelineRow label="Extraction" metric={data.extraction} />
            <PipelineRow label="Indexing" metric={data.indexing} />
            <PipelineRow label="Retrieval" metric={data.retrieval} />
            <PipelineRow label="Answer model" metric={data.model} />
          </tbody></table></div>
        </article>
        <article className="panel answer-outcomes">
          <div className="panel-heading"><div><h2>Answer outcomes</h2><p>Persisted terminal statuses since startup.</p></div></div>
          <dl><Outcome label="Grounded" value={data.grounded} /><Outcome label="Insufficient evidence" value={data.insufficient} /><Outcome label="Technical failure" value={data.technicalFailures} /></dl>
        </article>
      </div>
      <p className="operations-note">Metrics reset when QIP restarts. Historical dashboards and alerts remain deferred to a later deployment milestone.</p>
    </> : null}
  </section>;
}

function pipeline(total: MetricSnapshot, failures: MetricSnapshot): PipelineMetric {
  const count = measurement(total, "COUNT");
  const totalSeconds = measurement(total, "TOTAL_TIME");
  return {
    total: count,
    failures: measurement(failures, "COUNT"),
    averageMs: count === 0 ? 0 : totalSeconds * 1000 / count,
    maxMs: measurement(total, "MAX") * 1000,
  };
}

function measurement(metric: MetricSnapshot, statistic: string) {
  return metric.measurements.find((item) => item.statistic === statistic)?.value ?? 0;
}

function Summary({ label, value, state }: { label: string; value: string | number; state?: string }) {
  return <article className={"panel operations-card " + (state ? "operations-card-" + state : "")}><span>{label}</span><strong>{value}</strong></article>;
}

function PipelineRow({ label, metric }: { label: string; metric: PipelineMetric }) {
  const failureRate = metric.total === 0 ? 0 : metric.failures * 100 / metric.total;
  return <tr><th scope="row">{label}</th><td>{metric.total}</td><td>{metric.failures}</td><td>{failureRate.toFixed(1)}%</td><td>{formatDuration(metric.averageMs)}</td><td>{formatDuration(metric.maxMs)}</td></tr>;
}

function Outcome({ label, value }: { label: string; value: number }) {
  return <div><dt>{label}</dt><dd>{value}</dd></div>;
}

function formatDuration(milliseconds: number) {
  return milliseconds >= 1000 ? (milliseconds / 1000).toFixed(2) + " s" : milliseconds.toFixed(1) + " ms";
}