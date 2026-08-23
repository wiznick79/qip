import { useEffect, useState, type FormEvent } from "react";
import { api } from "./api";
import { ErrorNotice } from "./components/Feedback";
import { AssetsPage } from "./pages/AssetsPage";
import { DashboardPage } from "./pages/DashboardPage";
import { DocumentsPage } from "./pages/DocumentsPage";
import { IncidentsPage } from "./pages/IncidentsPage";
import { InvestigationsPage } from "./pages/InvestigationsPage";
import { IncidentWorkspacePage } from "./pages/IncidentWorkspacePage";
import type { UserSession } from "./types";

type View = "dashboard" | "assets" | "incidents" | "documents" | "investigations";

const navigation: { id: View; label: string; number: string }[] = [
  { id: "dashboard", label: "Dashboard", number: "00" },
  { id: "assets", label: "Assets", number: "01" },
  { id: "incidents", label: "Incidents", number: "02" },
  { id: "documents", label: "Documents", number: "03" },
  { id: "investigations", label: "Investigate", number: "04" },
];

export function App() {
  const [location, setLocation] = useState(readLocation);
  const [session, setSession] = useState<UserSession | null>(null);
  const [sessionError, setSessionError] = useState<string | null>(null);

  useEffect(() => {
    const synchronize = () => setLocation(readLocation());
    window.addEventListener("popstate", synchronize);
    window.addEventListener("hashchange", synchronize);
    return () => {
      window.removeEventListener("popstate", synchronize);
      window.removeEventListener("hashchange", synchronize);
    };
  }, []);

  useEffect(() => {
    let active = true;
    api.getSession()
      .then((current) => active && setSession(current))
      .catch((cause) => active && setSessionError(message(cause)));
    return () => { active = false; };
  }, []);

  function navigate(view: View, incidentId?: string) {
    const query = incidentId ? `?incident=${encodeURIComponent(incidentId)}` : "";
    window.history.pushState(null, "", `#/${view}${query}`);
    setLocation({ view, incidentId: incidentId ?? null });
  }

  async function login(username: string, password: string) {
    setSessionError(null);
    try {
      const authenticated = await api.login(username, password);
      setSession(authenticated);
      if (authenticated.authenticated) navigate("dashboard");
    }
    catch (cause) { setSessionError(message(cause)); }
  }

  async function logout() {
    setSessionError(null);
    try { setSession(await api.logout()); }
    catch (cause) { setSessionError(message(cause)); }
  }

  if (!session) {
    return <div className="session-screen"><div className="session-card"><span className="brand-mark" aria-hidden="true"><i /><i /><i /></span><p>Loading secure workspace…</p><ErrorNotice message={sessionError} /></div></div>;
  }

  if (!session.authenticated) {
    return <LoginPage error={sessionError} onLogin={login} />;
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <a className="brand" href="#/dashboard" aria-label="QIP home" onClick={(event) => { event.preventDefault(); navigate("dashboard"); }}>
          <span className="brand-mark" aria-hidden="true"><i /><i /><i /></span>
          <span><strong>QIP</strong><small>Quality Investigation Platform</small></span>
        </a>
        <div className="user-session"><span><strong>{session.username}</strong><small>{session.roles.join(" · ")}</small></span><button className="quiet-button" onClick={logout}>Sign out</button></div>
      </header>
      <div className="workspace">
        <aside className="sidebar">
          <div><p className="nav-label">Workspace</p><nav aria-label="Primary navigation">
            {navigation.map((item) => <button key={item.id} className={location.view === item.id ? "active" : ""} onClick={() => navigate(item.id)} aria-current={location.view === item.id ? "page" : undefined}><span>{item.number}</span>{item.label}</button>)}
          </nav></div>
          <div className="grounding-note"><span aria-hidden="true">◎</span><div><strong>Evidence first</strong><p>AI findings will remain suggestions until a person confirms them.</p></div></div>
        </aside>
        <main id="main">
          {location.view === "dashboard" ? <DashboardPage onNavigate={navigate} onOpenIncident={(incidentId) => navigate("incidents", incidentId)} />
            : location.view === "assets" ? <AssetsPage />
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
                  session={session}
                />}
        </main>
      </div>
    </div>
  );
}

function LoginPage({ error, onLogin }: { error: string | null; onLogin: (username: string, password: string) => Promise<void> }) {
  const [submitting, setSubmitting] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setSubmitting(true);
    try { await onLogin(String(form.get("username")), String(form.get("password"))); }
    finally { setSubmitting(false); }
  }

  return <div className="session-screen">
    <section className="session-card panel" aria-labelledby="login-title">
      <div className="session-brand"><span className="brand-mark" aria-hidden="true"><i /><i /><i /></span><div><strong>QIP</strong><small>Quality Investigation Platform</small></div></div>
      <div><h1 id="login-title">Sign in</h1></div>
      <ErrorNotice message={error} />
      <form onSubmit={submit}>
        <label>Username<input name="username" autoComplete="username" required autoFocus /></label>
        <label>Password<input name="password" type="password" autoComplete="current-password" required /></label>
        <button className="primary-button" disabled={submitting}>{submitting ? "Signing in…" : "Sign in"}</button>
      </form>
    </section>
  </div>;
}

function readLocation(): { view: View; incidentId: string | null } {
  const [path, query = ""] = window.location.hash.replace(/^#\/?/, "").split("?");
  const view = navigation.some((item) => item.id === path) ? path as View : "dashboard";
  return { view, incidentId: new URLSearchParams(query).get("incident") };
}

function message(cause: unknown) {
  return cause instanceof Error ? cause.message : "The secure workspace could not be loaded.";
}
