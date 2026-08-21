import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { IncidentsPage } from "./IncidentsPage";

const reported = incident("incident-reported", "Reported seal leak", "REPORTED");
const investigating = incident("incident-investigating", "Pump vibration", "INVESTIGATING");
const resolved = incident("incident-resolved", "Resolved conveyor stop", "RESOLVED");

describe("Incident queue lifecycle", () => {
  beforeEach(() => {
    vi.spyOn(globalThis, "fetch").mockImplementation(async (input, init) => {
      const url = String(input);
      if (url.startsWith("/api/assets?")) return json({ items: [{ id: "asset-1", name: "Atlas Press", type: "MACHINE", externalReference: null, createdAt: "2026-08-20T08:00:00Z" }], page: 0, size: 100, totalElements: 1 });
      if (url.startsWith("/api/incidents?") && new URL(url, "http://qip.local").searchParams.get("page") === "1") {
        return json({ items: [resolved], page: 1, size: 20, totalElements: 21 });
      }
      if (url.startsWith("/api/incidents?")) return json({ items: [reported, investigating], page: 0, size: 20, totalElements: 21 });
      if (url.endsWith("/status") && init?.method === "PATCH") {
        const requested = JSON.parse(String(init.body)) as { status: string };
        const current = url.includes("reported") ? reported : investigating;
        return json({ ...current, status: requested.status });
      }
      return new Response(null, { status: 404 });
    });
  });

  afterEach(() => vi.restoreAllMocks());

  it("paginates the full queue using the backend total", async () => {
    render(<IncidentsPage />);
    expect(await screen.findByText("Reported seal leak")).toBeInTheDocument();
    expect(screen.getByText("Page 1 of 2 · 21 incidents")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Next" }));
    expect(await screen.findByText("Resolved conveyor stop")).toBeInTheDocument();
    expect(screen.getByText("Page 2 of 2 · 21 incidents")).toBeInTheDocument();
  });

  it("starts an investigation explicitly and supports a separate resolution action", async () => {
    const investigate = vi.fn();
    render(<IncidentsPage onInvestigate={investigate} />);
    await screen.findByText("Reported seal leak");

    fireEvent.click(screen.getByRole("button", { name: "Start investigation" }));
    await waitFor(() => expect(investigate).toHaveBeenCalledWith("incident-reported"));

    fireEvent.click(screen.getByRole("button", { name: "Mark resolved" }));
    await waitFor(() => expect(globalThis.fetch).toHaveBeenCalledWith(
      "/api/incidents/incident-investigating/status",
      expect.objectContaining({ method: "PATCH", body: JSON.stringify({ status: "RESOLVED" }) }),
    ));
  });
});

function incident(id: string, title: string, status: string) {
  return { id, assetId: "asset-1", title, description: "Synthetic incident", severity: "HIGH", status,
    occurredAt: "2026-08-20T09:00:00Z", createdAt: "2026-08-20T10:00:00Z", updatedAt: "2026-08-20T10:00:00Z" };
}

function json(value: unknown) {
  return new Response(JSON.stringify(value), { status: 200, headers: { "Content-Type": "application/json" } });
}
