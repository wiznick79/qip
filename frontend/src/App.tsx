import { useEffect, useState } from "react";
import { AssetsPage } from "./pages/AssetsPage";
import { DocumentsPage } from "./pages/DocumentsPage";
import { IncidentsPage } from "./pages/IncidentsPage";
import { InvestigationsPage } from "./pages/InvestigationsPage";
import { IncidentWorkspacePage } from "./pages/IncidentWorkspacePage";

type View = "assets" | "incidents" | "documents" | "investigations";

const navigation: { id: View; label: string; number: string }[] = [
  { id: "assets", label: "Assets", number: "01" },
  { id: "incidents", label: "Incidents", number: "02" },
  { id: "documents", label: "Documents", number: "03" },
  { id: "investigations", label: "Investigate", number: "04" },
];

export function App() {
  const [location, setLocation] = useState(readLocation);

  useEffect(() => {
    const synchronize = () => setLocation(readLocation());
    window.addEventListener("popstate", synchronize);
    window.addEventListener("hashchange", synchronize);
    return () => {
      window.removeEventListener("popstate", synchronize);
      window.removeEventListener("hashchange", synchronize);
    };
  }, []);

  function navigate(view: View, incidentId?: string) {
    const query = incidentId ? `?incident=${encodeURIComponent(incidentId)}` : "";
    window.history.pushState(null, "", `#/${view}${query}`);
    setLocation({ view, incidentId: incidentId ?? null });
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <a className="brand" href="#/assets" aria-label="QIP home" onClick={(event) => { event.preventDefault(); navigate("assets"); }}>
          <span className="brand-mark" aria-hidden="true"><i /><i /><i /></span>
          <span><strong>QIP</strong><small>Quality Investigation Platform</small></span>
        </a>
        <div className="system-state"><span />Decision support · Local workspace</div>
      </header>
      <div className="workspace">
        <aside className="sidebar">
          <div><p className="nav-label">Workspace</p><nav aria-label="Primary navigation">
            {navigation.map((item) => <button key={item.id} className={location.view === item.id ? "active" : ""} onClick={() => navigate(item.id)} aria-current={location.view === item.id ? "page" : undefined}><span>{item.number}</span>{item.label}</button>)}
          </nav></div>
          <div className="grounding-note"><span aria-hidden="true">◎</span><div><strong>Evidence first</strong><p>AI findings will remain suggestions until a person confirms them.</p></div></div>
        </aside>
        <main id="main">
          {location.view === "assets" ? <AssetsPage />
            : location.view === "incidents" ? location.incidentId
              ? <IncidentWorkspacePage
                incidentId={location.incidentId}
                onBack={() => navigate("incidents")}
                onInvestigate={(incidentId) => navigate("investigations", incidentId)}
              />
              : <IncidentsPage
                onOpenCase={(incidentId) => navigate("incidents", incidentId)}
                onInvestigate={(incidentId) => navigate("investigations", incidentId)}
              />
              : location.view === "documents" ? <DocumentsPage />
                : <InvestigationsPage
                  initialIncidentId={location.incidentId}
                  onInvestigationOpened={(incidentId) => navigate("investigations", incidentId)}
                  onViewAllIncidents={() => navigate("incidents")}
                />}
        </main>
      </div>
    </div>
  );
}

function readLocation(): { view: View; incidentId: string | null } {
  const [path, query = ""] = window.location.hash.replace(/^#\/?/, "").split("?");
  const view = navigation.some((item) => item.id === path) ? path as View : "assets";
  return { view, incidentId: new URLSearchParams(query).get("incident") };
}
