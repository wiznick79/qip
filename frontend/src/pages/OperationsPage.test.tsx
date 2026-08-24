import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { OperationsPage } from "./OperationsPage";

describe("Operations page", () => {
  beforeEach(() => {
    vi.spyOn(globalThis, "fetch").mockImplementation(async (input) => {
      const url = new URL(String(input), "http://qip.local");
      if (url.pathname === "/actuator/health") return json({ status: "UP" });
      if (!url.pathname.startsWith("/actuator/metrics/")) return new Response(null, { status: 404 });

      const name = decodeURIComponent(url.pathname.replace("/actuator/metrics/", ""));
      const tags = new Map(url.searchParams.getAll("tag").map((tag) => {
        const separator = tag.indexOf(":");
        return [tag.slice(0, separator), tag.slice(separator + 1)];
      }));
      if (name === "qip.knowledge.ingestion" && tags.get("stage") === "extraction") {
        return tags.get("outcome") === "failure" ? timer(1, 0.1, 0.1) : timer(5, 0.8, 0.4);
      }
      if (name === "qip.knowledge.ingestion" && tags.get("stage") === "indexing") {
        return tags.get("outcome") === "failure" ? timer(0, 0, 0) : timer(3, 0.3, 0.2);
      }
      if (name === "qip.knowledge.retrieval") {
        return tags.get("outcome") === "failure" ? timer(1, 0.2, 0.2) : timer(4, 2, 1);
      }
      if (name === "qip.investigations.model") {
        return tags.get("outcome") === "failure" ? timer(1, 2, 2) : timer(2, 6, 4);
      }
      if (name === "qip.investigations.answers") {
        const counts: Record<string, number> = {
          GROUNDED: 2,
          INSUFFICIENT_EVIDENCE: 1,
          TECHNICAL_FAILURE: 1,
        };
        return counter(counts[tags.get("status") ?? ""] ?? 0);
      }
      return new Response(null, { status: 404 });
    });
  });

  afterEach(() => vi.restoreAllMocks());

  it("summarizes current-process health, failures, latency, and answer outcomes", async () => {
    render(<OperationsPage />);

    expect(await screen.findByText("UP")).toBeInTheDocument();
    expect(screen.getByText("Ingestion failures").parentElement).toHaveTextContent("1");
    expect(screen.getByText("Grounded answers").parentElement).toHaveTextContent("2");

    const extraction = screen.getByRole("row", { name: /Extraction/ });
    expect(within(extraction).getAllByRole("cell").map((cell) => cell.textContent))
      .toEqual(["5", "1", "20.0%", "160.0 ms", "400.0 ms"]);

    const model = screen.getByRole("row", { name: /Answer model/ });
    expect(within(model).getAllByRole("cell").map((cell) => cell.textContent))
      .toEqual(["2", "1", "50.0%", "3.00 s", "4.00 s"]);

    expect(screen.getByText("Metrics reset when QIP restarts. Historical dashboards and alerts remain deferred to a later deployment milestone.")).toBeInTheDocument();
  });

  it("refreshes every displayed measurement on demand", async () => {
    const fetchMock = vi.mocked(globalThis.fetch);
    render(<OperationsPage />);
    await screen.findByText("UP");
    expect(fetchMock).toHaveBeenCalledTimes(12);

    fireEvent.click(screen.getByRole("button", { name: "Refresh" }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(24));
    expect(screen.getByRole("button", { name: "Refresh" })).toBeEnabled();
  });

  it("shows a safe error when metrics cannot be loaded", async () => {
    vi.mocked(globalThis.fetch).mockImplementation(async () => new Response(JSON.stringify({
      title: "Operational metrics unavailable",
      detail: "Metrics unavailable.",
      status: 503,
    }), { status: 503, headers: { "Content-Type": "application/problem+json" } }));

    render(<OperationsPage />);

    expect(await screen.findByRole("alert")).toHaveTextContent("Metrics unavailable.");
  });
});

function timer(count: number, totalTime: number, max: number) {
  return json({
    name: "timer",
    description: "Synthetic timer",
    baseUnit: "seconds",
    measurements: [
      { statistic: "COUNT", value: count },
      { statistic: "TOTAL_TIME", value: totalTime },
      { statistic: "MAX", value: max },
    ],
  });
}

function counter(count: number) {
  return json({
    name: "counter",
    description: "Synthetic counter",
    baseUnit: null,
    measurements: [{ statistic: "COUNT", value: count }],
  });
}

function json(value: unknown) {
  return new Response(JSON.stringify(value), { status: 200, headers: { "Content-Type": "application/json" } });
}