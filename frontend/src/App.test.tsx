import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { App } from "./App";

describe("QIP workspace", () => {
  beforeEach(() => {
    window.history.replaceState(null, "", "#/assets");
    vi.spyOn(globalThis, "fetch").mockImplementation(async (input, init) => {
      const url = String(input);
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

  it("states the human confirmation boundary", () => {
    render(<App />);
    expect(screen.getByText(/AI findings will remain suggestions/i)).toBeInTheDocument();
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
    expect(screen.getByText(/not sent to the AI model/i)).toBeInTheDocument();
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
