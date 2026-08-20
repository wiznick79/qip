import { useCallback, useEffect, useState, type FormEvent } from "react";
import { api } from "../api";
import { EmptyState, ErrorNotice, LoadingRows } from "../components/Feedback";
import type { SourceDocument } from "../types";
import { PageHeading } from "./AssetsPage";

export function DocumentsPage() {
  const [documents, setDocuments] = useState<SourceDocument[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true); setError(null);
    try { setDocuments((await api.listDocuments()).items); }
    catch (cause) { setError(message(cause)); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => void load(), [load]);
  useEffect(() => {
    if (!documents.some((document) => document.status === "UPLOADED" || document.status === "EXTRACTING")) return;
    const timer = window.setInterval(() => void load(), 2_000);
    return () => window.clearInterval(timer);
  }, [documents, load]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const form = new FormData(event.currentTarget); const file = form.get("file");
    if (!(file instanceof File) || file.size === 0) { setError("Choose a PDF or plain-text file."); return; }
    setUploading(true); setError(null);
    try { await api.uploadDocument(String(form.get("title")), file); event.currentTarget.reset(); await load(); }
    catch (cause) { setError(message(cause)); }
    finally { setUploading(false); }
  }

  return <section className="page" aria-labelledby="documents-title">
    <PageHeading eyebrow="Knowledge sources" title="Documents" detail="Upload controlled technical sources and monitor text extraction." count={documents.length} />
    <ErrorNotice message={error} />
    <div className="split-layout">
      <article className="panel form-panel">
        <div className="panel-heading"><span className="step">03</span><div><h2>Upload a source</h2><p>PDF or UTF-8 text, up to 10 MiB.</p></div></div>
        <form onSubmit={submit}>
          <label>Document title<input name="title" required maxLength={200} placeholder="e.g. Synthetic bearing service guide" /></label>
          <label className="file-field">Source file<input name="file" type="file" required accept="application/pdf,text/plain,.pdf,.txt" /><span>Content is treated as untrusted evidence, never as instructions.</span></label>
          <button className="primary-button" disabled={uploading}>{uploading ? "Uploading & extracting…" : "Upload document"}</button>
        </form>
      </article>
      <article className="panel list-panel">
        <div className="panel-heading filter-heading"><div><h2>Document library</h2><p>Latest uploads and extraction state.</p></div><button className="quiet-button" onClick={() => void load()} disabled={loading}>Refresh</button></div>
        {loading ? <LoadingRows columns={4} /> : documents.length === 0 ? <EmptyState title="No documents yet" detail="Upload a technical source to begin building the knowledge base." /> : <div className="records">{documents.map((document) => <div className="record document-record" key={document.id}><div className="file-icon">{document.mediaType === "application/pdf" ? "PDF" : "TXT"}</div><div className="record-main"><strong>{document.title}</strong><span>{document.originalFilename} · {formatBytes(document.sizeBytes)} · {document.extractedPageCount} page{document.extractedPageCount === 1 ? "" : "s"}</span>{document.failureReason ? <small>{document.failureReason}</small> : null}</div><span className={`status status-${document.status.toLowerCase().replace("_", "-")}`}>{document.status.replace("_", " ")}</span></div>)}</div>}
      </article>
    </div>
  </section>;
}

function formatBytes(bytes: number) { return bytes < 1024 ? `${bytes} B` : `${(bytes / 1024).toFixed(1)} KiB`; }
function message(cause: unknown) { return cause instanceof Error ? cause.message : "Something went wrong."; }
