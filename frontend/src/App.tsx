import { useState } from "react";
import { AssetsPage } from "./pages/AssetsPage";
import { DocumentsPage } from "./pages/DocumentsPage";
import { IncidentsPage } from "./pages/IncidentsPage";
import { InvestigationsPage } from "./pages/InvestigationsPage";

type View = "assets" | "incidents" | "documents" | "investigations";

const navigation: { id: View; label: string; number: string }[] = [
  { id: "assets", label: "Assets", number: "01" },
  { id: "incidents", label: "Incidents", number: "02" },
  { id: "documents", label: "Documents", number: "03" },
  { id: "investigations", label: "Investigate", number: "04" },
];

export function App() {
  const [view, setView] = useState<View>("assets");

  return (
    <div className="app-shell">
      <header className="topbar">
        <a className="brand" href="#main" aria-label="QIP home">
          <span className="brand-mark" aria-hidden="true"><i /><i /><i /></span>
          <span><strong>QIP</strong><small>Quality Investigation Platform</small></span>
        </a>
        <div className="system-state"><span />Decision support · Local workspace</div>
      </header>
      <div className="workspace">
        <aside className="sidebar">
          <div><p className="nav-label">Workspace</p><nav aria-label="Primary navigation">
            {navigation.map((item) => <button key={item.id} className={view === item.id ? "active" : ""} onClick={() => setView(item.id)} aria-current={view === item.id ? "page" : undefined}><span>{item.number}</span>{item.label}</button>)}
          </nav></div>
          <div className="grounding-note"><span aria-hidden="true">◎</span><div><strong>Evidence first</strong><p>AI findings will remain suggestions until a person confirms them.</p></div></div>
        </aside>
        <main id="main">
          {view === "assets" ? <AssetsPage />
            : view === "incidents" ? <IncidentsPage />
              : view === "documents" ? <DocumentsPage />
                : <InvestigationsPage onViewAllIncidents={() => setView("incidents")} />}
        </main>
      </div>
    </div>
  );
}
