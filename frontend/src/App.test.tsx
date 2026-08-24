import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { App } from "./App";

describe("QIP workspace", () => {
  beforeEach(() => {
    window.history.replaceState(null, "", "#/assets");
    vi.spyOn(globalThis, "fetch").mockImplementation(async (input, init) => {
      const url = String(input);
      if (url === "/api/session") return jsonResponse({
        authenticated: true,
        username: "wiznick79",
        roles: ["ADMIN", "INVESTIGATOR", "REVIEWER"],
        csrfHeaderName: "X-CSRF-TOKEN",
        csrfToken: "test-csrf-token",
      });
      if (url === "/api/incidents/incident-1") return jsonResponse(incident);
      if (url.startsWith("/api/incidents/incident-1/observations")) return jsonResponse({ items: [], page: 0, size: 10, totalElements: 0 });
      if (url.startsWith("/api/incidents/incident-1/evidence")) return jsonResponse({ items: [], page: 0, size: 10, totalElements: 0 });
      if (url === "/api/incidents/incident-1/investigations" && init?.method === "POST") {
        return jsonResponse({ id: "investigation-1", incidentId: "incident-1", status: "OPEN", closureSummary: null,
          closedBy: null, closedAt: null, questions: [], findings: [], createdAt: "2026-08-20T10:00:00Z",
          updatedAt: "2026-08-20T10:00:00Z" });
      }
      if (url.startsWith("/api/assets")) {
        return jsonResponse({ items: [{
          id: "asset-1", name: "Synthetic Press", type: "MACHINE", externalReference: "PRESS-01",
          createdAt: "2026-08-20T10:00:00Z",
        }], page: 0, size: 100, totalElements: 1 });
      }
      if (url.startsWith("/api/incidents")) {
        return jsonResponse({ items: [], page: 0, size: 100, totalElements: 0 });
      }
      if (url.startsWith("/api/documents")) {
        return jsonResponse({ items: [{
          id: "document-1", title: "Synthetic guide", originalFilename: "guide.txt", mediaType: "text/plain",
          sizeBytes: 120, checksumSha256: "a".repeat(64), status: "INDEXED", failureReason: null,
          extractedPageCount: 1, uploadedAt: "2026-08-20T10:00:00Z", updatedAt: "2026-08-20T10:00:01Z",
        }], page: 0, size: 100, totalElements: 1 });
      }
      return new Response(null, { status: 404 });
    });
  });

  afterEach(() => vi.restoreAllMocks());

  it("navigates between the primary workflow screens", async () => {
    render(<App />);
    expect(await screen.findByText("Synthetic Press")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /incidents/i }));
    expect(await screen.findByRole("heading", { name: "Incident queue" })).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /documents/i }));
    expect(await screen.findByText("Synthetic guide")).toBeInTheDocument();
    expect(screen.getByText("INDEXED")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /investigate/i }));
    expect(await screen.findByRole("heading", { name: "Grounded questions" })).toBeInTheDocument();
  });

  it("uses the dashboard as the application home", async () => {
    window.history.replaceState(null, "", "#/");
    render(<App />);

    expect(await screen.findByRole("heading", { name: "Dashboard" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /report an incident/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /open investigations/i })).toBeInTheDocument();
  });

  it("shows Operations only to administrators and does not call metrics for other roles", async () => {
    const adminView = render(<App />);
    expect(await screen.findByRole("button", { name: /Operations/ })).toBeInTheDocument();
    adminView.unmount();

    vi.restoreAllMocks();
    window.history.replaceState(null, "", "#/operations");
    const fetchMock = vi.spyOn(globalThis, "fetch").mockImplementation(async (input) => {
      const url = String(input);
      if (url === "/api/session") return jsonResponse({
        authenticated: true,
        username: "qip-investigator",
        roles: ["INVESTIGATOR"],
        csrfHeaderName: "X-CSRF-TOKEN",
        csrfToken: "test-csrf-token",
      });
      if (url.startsWith("/api/assets")) return jsonResponse({ items: [], page: 0, size: 100, totalElements: 0 });
      if (url.startsWith("/api/incidents")) return jsonResponse({ items: [], page: 0, size: 5, totalElements: 0 });
      if (url.startsWith("/api/documents")) return jsonResponse({ items: [], page: 0, size: 100, totalElements: 0 });
      return new Response(null, { status: 404 });
    });

    render(<App />);

    expect(await screen.findByRole("heading", { name: "Dashboard" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Operations/ })).not.toBeInTheDocument();
    expect(fetchMock.mock.calls.some(([input]) => String(input).startsWith("/actuator/metrics"))).toBe(false);
  });

  it("signs in with valid credentials and opens the dashboard", async () => {
    let authenticated = false;
    vi.mocked(globalThis.fetch).mockImplementation(async (input, init) => {
      const url = String(input);
      if (url === "/api/session/login" && init?.method === "POST") {
        authenticated = true;
        return new Response(null, { status: 204 });
      }
      if (url === "/api/session") return jsonResponse(authenticated
        ? {
            authenticated: true,
            username: "qip-investigator",
            roles: ["INVESTIGATOR"],
            csrfHeaderName: "X-CSRF-TOKEN",
            csrfToken: "signed-in-csrf-token",
          }
        : {
            authenticated: false,
            username: null,
            roles: [],
            csrfHeaderName: "X-CSRF-TOKEN",
            csrfToken: "anonymous-csrf-token",
          });
      return new Response(null, { status: 404 });
    });

    render(<App />);
    fireEvent.change(await screen.findByLabelText("Username"), { target: { value: "qip-investigator" } });
    fireEvent.change(screen.getByLabelText("Password"), { target: { value: "local-password" } });
    fireEvent.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByRole("heading", { name: "Dashboard" })).toBeInTheDocument();
    expect(screen.getByText("qip-investigator")).toBeInTheDocument();
  });

  it("shows the server error when credentials are invalid", async () => {
    vi.mocked(globalThis.fetch).mockImplementation(async (input, init) => {
      const url = String(input);
      if (url === "/api/session") return jsonResponse({
        authenticated: false,
        username: null,
        roles: [],
        csrfHeaderName: "X-CSRF-TOKEN",
        csrfToken: "anonymous-csrf-token",
      });
      if (url === "/api/session/login" && init?.method === "POST") {
        return new Response(JSON.stringify({
          title: "Authentication failed",
          detail: "The username or password is incorrect.",
          status: 401,
        }), { status: 401, headers: { "Content-Type": "application/problem+json" } });
      }
      return new Response(null, { status: 404 });
    });

    render(<App />);
    fireEvent.change(await screen.findByLabelText("Username"), { target: { value: "qip-investigator" } });
    fireEvent.change(screen.getByLabelText("Password"), { target: { value: "wrong-password" } });
    fireEvent.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("The username or password is incorrect.");
    expect(screen.getByRole("heading", { name: "Sign in" })).toBeInTheDocument();
  });

  it("shows the sign-in screen when the restored session has expired", async () => {
    vi.mocked(globalThis.fetch).mockImplementation(async (input) => {
      if (String(input) === "/api/session") return jsonResponse({
        authenticated: false,
        username: null,
        roles: [],
        csrfHeaderName: "X-CSRF-TOKEN",
        csrfToken: "anonymous-csrf-token",
      });
      return new Response(null, { status: 404 });
    });

    render(<App />);

    expect(await screen.findByRole("heading", { name: "Sign in" })).toBeInTheDocument();
    expect(screen.queryByRole("navigation", { name: "Primary navigation" })).not.toBeInTheDocument();
  });
  it("returns to the sign-in screen after logout", async () => {
    let authenticated = true;
    vi.mocked(globalThis.fetch).mockImplementation(async (input, init) => {
      const url = String(input);
      if (url === "/api/session/logout" && init?.method === "POST") {
        authenticated = false;
        return new Response(null, { status: 204 });
      }
      if (url === "/api/session") return jsonResponse(authenticated
        ? {
            authenticated: true,
            username: "qip-investigator",
            roles: ["INVESTIGATOR"],
            csrfHeaderName: "X-CSRF-TOKEN",
            csrfToken: "authenticated-csrf-token",
          }
        : {
            authenticated: false,
            username: null,
            roles: [],
            csrfHeaderName: "X-CSRF-TOKEN",
            csrfToken: "anonymous-csrf-token",
          });
      if (url.startsWith("/api/assets")) return jsonResponse({ items: [], page: 0, size: 100, totalElements: 0 });
      if (url.startsWith("/api/incidents")) return jsonResponse({ items: [], page: 0, size: 100, totalElements: 0 });
      return new Response(null, { status: 404 });
    });

    render(<App />);
    fireEvent.click(await screen.findByRole("button", { name: "Sign out" }));

    expect(await screen.findByRole("heading", { name: "Sign in" })).toBeInTheDocument();
    expect(screen.queryByText("qip-investigator")).not.toBeInTheDocument();
  });
  it("states the human confirmation boundary", async () => {
    render(<App />);
    expect(await screen.findByText(/AI findings will remain suggestions/i)).toBeInTheDocument();
    expect(screen.getByText("wiznick79")).toBeInTheDocument();
  });

  it("opens a bookmarkable incident investigation URL", async () => {
    window.history.replaceState(null, "", "#/investigations?incident=incident-1");
    render(<App />);

    expect(await screen.findAllByText("Synthetic vibration")).toHaveLength(2);
    expect(screen.getByText("No questions yet")).toBeInTheDocument();
    expect(window.location.hash).toBe("#/investigations?incident=incident-1");
  });

  it("opens a bookmarkable incident record URL", async () => {
    window.history.replaceState(null, "", "#/incidents?incident=incident-1");
    render(<App />);

    expect(await screen.findByRole("heading", { name: "Synthetic vibration" })).toBeInTheDocument();
    expect(screen.getByText(/not automatically included in AI searches/i)).toBeInTheDocument();
    expect(window.location.hash).toBe("#/incidents?incident=incident-1");
  });

  it("resets the incident form after a successful asynchronous submission", async () => {
    render(<App />);
    expect(await screen.findByText("Synthetic Press")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /incidents/i }));
    await screen.findByRole("heading", { name: "Incident queue" });

    fireEvent.change(screen.getByLabelText("Asset"), { target: { value: "asset-1" } });
    fireEvent.change(screen.getByLabelText("Title"), { target: { value: "Synthetic vibration" } });
    fireEvent.change(screen.getByLabelText("Occurred at"), { target: { value: "2026-08-20T18:00" } });
    fireEvent.click(screen.getByRole("button", { name: "Report incident" }));

    await waitFor(() => expect(screen.getByLabelText("Title")).toHaveValue(""));
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });
});

const incident = {
  id: "incident-1", assetId: "asset-1", title: "Synthetic vibration", description: "Test incident",
  severity: "MEDIUM", status: "INVESTIGATING", occurredAt: "2026-08-20T09:00:00Z",
  createdAt: "2026-08-20T10:00:00Z", updatedAt: "2026-08-20T10:00:00Z",
};

function jsonResponse(value: unknown) {
  return new Response(JSON.stringify(value), { status: 200, headers: { "Content-Type": "application/json" } });
}
