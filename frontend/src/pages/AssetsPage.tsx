import { useCallback, useEffect, useState, type FormEvent } from "react";
import { api } from "../api";
import { EmptyState, ErrorNotice, LoadingRows } from "../components/Feedback";
import type { Asset, AssetType } from "../types";

export function AssetsPage() {
  const [assets, setAssets] = useState<Asset[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setAssets((await api.listAssets()).items);
    } catch (cause) {
      setError(message(cause));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => void load(), [load]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    setSaving(true);
    setError(null);
    try {
      await api.createAsset({
        name: String(form.get("name")),
        type: String(form.get("type")) as AssetType,
        externalReference: String(form.get("externalReference") || "") || null,
      });
      formElement.reset();
      await load();
    } catch (cause) {
      setError(message(cause));
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="page" aria-labelledby="assets-title">
      <PageHeading
        eyebrow="Equipment registry"
        title="Assets"
        detail="Register the machines, tools, and lines that anchor every investigation."
        count={assets.length}
      />
      <ErrorNotice message={error} />
      <div className="split-layout">
        <article className="panel form-panel">
          <div className="panel-heading">
            <span className="step">01</span>
            <div><h2>Add an asset</h2><p>Create a stable reference for future incidents.</p></div>
          </div>
          <form onSubmit={submit}>
            <label>Name<input name="name" required maxLength={120} placeholder="e.g. Forming Press 04" /></label>
            <label>Asset type
              <select name="type" defaultValue="MACHINE">
                <option value="MACHINE">Machine</option><option value="PRODUCTION_LINE">Production line</option>
                <option value="TOOL">Tool</option><option value="OTHER">Other</option>
              </select>
            </label>
            <label>External reference <span className="optional">Optional</span>
              <input name="externalReference" maxLength={100} placeholder="e.g. PRESS-04" />
            </label>
            <button className="primary-button" disabled={saving}>{saving ? "Registering…" : "Register asset"}</button>
          </form>
        </article>
        <article className="panel list-panel">
          <div className="panel-heading"><div><h2>Registered assets</h2><p>Sorted by name for quick scanning.</p></div></div>
          {loading ? <LoadingRows /> : assets.length === 0 ? (
            <EmptyState title="No assets yet" detail="Register the first asset to begin an investigation." />
          ) : (
            <div className="records">
              {assets.map((asset) => <div className="record" key={asset.id}>
                <div className="record-icon">{asset.name.slice(0, 2).toUpperCase()}</div>
                <div className="record-main"><strong>{asset.name}</strong><span>{label(asset.type)}</span></div>
                <code>{asset.externalReference ?? "No reference"}</code>
              </div>)}
            </div>
          )}
        </article>
      </div>
    </section>
  );
}

export function PageHeading({ eyebrow, title, detail, count }: { eyebrow: string; title: string; detail: string; count: number }) {
  return <header className="page-heading"><div><p className="eyebrow">{eyebrow}</p><h1 id={`${title.toLowerCase()}-title`}>{title}</h1><p>{detail}</p></div><div className="metric"><strong>{count}</strong><span>in view</span></div></header>;
}

function label(value: string) { return value.toLowerCase().replaceAll("_", " ").replace(/^./, (letter) => letter.toUpperCase()); }
function message(cause: unknown) { return cause instanceof Error ? cause.message : "Something went wrong."; }
