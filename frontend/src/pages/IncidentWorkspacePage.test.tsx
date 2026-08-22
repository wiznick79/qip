import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { IncidentWorkspacePage } from "./IncidentWorkspacePage";

describe("Incident evidence workspace", () => {
  beforeEach(() => {
    vi.spyOn(globalThis, "fetch").mockImplementation(async (input, init) => {
      const url = String(input);
      if (url === "/api/incidents/incident-1") return json(incident);
      if (url.startsWith("/api/assets")) return json({ items: [asset], page: 0, size: 100, totalElements: 1 });
      if (url.includes("/observations") && init?.method === "POST") return json(observation);
      if (url.includes("/observations")) return json({ items: [observation], page: 0, size: 10, totalElements: 1 });
      if (url.includes("/evidence") && init?.method === "POST") return json(evidence);
      if (url.includes("/evidence")) return json({ items: [evidence], page: 0, size: 10, totalElements: 1 });
      return new Response(null, { status: 404 });
    });
  });

  afterEach(() => vi.restoreAllMocks());

  it("shows attributed append-only observations and evidence outside AI context", async () => {
    render(<IncidentWorkspacePage incidentId="incident-1" />);

    expect(await screen.findByText("Oil temperature reached 66 C")).toBeInTheDocument();
    expect(screen.getByText("Return-line gauge PT-14")).toBeInTheDocument();
    expect(screen.getByText(/not sent to the AI model/i)).toBeInTheDocument();
    expect(screen.getByText(/HUMAN ENTERED · wiznick79/i)).toBeInTheDocument();
  });

  it("appends an observation with caller provenance", async () => {
    render(<IncidentWorkspacePage incidentId="incident-1" />);
    await screen.findByText("Oil temperature reached 66 C");

    fireEvent.change(screen.getByLabelText("Observation"), { target: { value: "Filter housing was warm." } });
    fireEvent.change(screen.getByLabelText("Observed at"), { target: { value: "2026-08-20T08:45" } });
    fireEvent.click(screen.getByRole("button", { name: "Record observation" }));

    await waitFor(() => expect(globalThis.fetch).toHaveBeenCalledWith(
      "/api/incidents/incident-1/observations",
      expect.objectContaining({ method: "POST", body: expect.stringContaining("Filter housing was warm.") }),
    ));
    expect(screen.getByLabelText("Observation")).toHaveValue("");
  });

  it("appends evidence without allowing the client to assign provenance", async () => {
    render(<IncidentWorkspacePage incidentId="incident-1" />);
    await screen.findByText("Return-line gauge PT-14");

    fireEvent.change(screen.getByLabelText("Summary"), { target: { value: "Differential pressure measured 3.1 bar." } });
    fireEvent.change(screen.getByLabelText("Source reference"), { target: { value: "Gauge PT-14" } });
    fireEvent.change(screen.getByLabelText("Event time"), { target: { value: "2026-08-20T08:46" } });
    fireEvent.click(screen.getByRole("button", { name: "Record evidence" }));

    await waitFor(() => {
      const call = vi.mocked(globalThis.fetch).mock.calls.find(([path, options]) => String(path).endsWith("/evidence") && options?.method === "POST");
      expect(call).toBeDefined();
      expect(JSON.parse(String(call?.[1]?.body))).toEqual(expect.objectContaining({ type: "MEASUREMENT", sourceReference: "Gauge PT-14" }));
      expect(JSON.parse(String(call?.[1]?.body))).not.toHaveProperty("provenance");
    });
  });
});

const incident = { id: "incident-1", assetId: "asset-1", title: "Synthetic heat event", description: "Slow retract observed.", severity: "HIGH", status: "INVESTIGATING", occurredAt: "2026-08-20T08:40:00Z", createdAt: "2026-08-20T09:00:00Z", updatedAt: "2026-08-20T09:00:00Z" };
const asset = { id: "asset-1", name: "Atlas HP-40", type: "MACHINE", externalReference: "SYN-HP-040", createdAt: "2026-08-20T08:00:00Z" };
const observation = { id: "observation-1", incidentId: "incident-1", text: "Oil temperature reached 66 C", authorReference: "wiznick79", observedAt: "2026-08-20T08:41:00Z", recordedAt: "2026-08-20T09:01:00Z" };
const evidence = { id: "evidence-1", incidentId: "incident-1", type: "MEASUREMENT", summary: "Return-filter differential was 3.1 bar", sourceReference: "Return-line gauge PT-14", eventAt: "2026-08-20T08:42:00Z", provenance: "HUMAN_ENTERED", submittedBy: "wiznick79", recordedAt: "2026-08-20T09:02:00Z" };
function json(value: unknown) { return new Response(JSON.stringify(value), { status: 200, headers: { "Content-Type": "application/json" } }); }
