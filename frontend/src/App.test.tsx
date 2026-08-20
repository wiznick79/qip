import { fireEvent, render, screen } from "@testing-library/react";
import { App } from "./App";

describe("QIP workspace", () => {
  beforeEach(() => {
    vi.spyOn(globalThis, "fetch").mockImplementation(async (input) => {
      const url = String(input);
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
          sizeBytes: 120, checksumSha256: "a".repeat(64), status: "EXTRACTED", failureReason: null,
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
    expect(screen.getByText("EXTRACTED")).toBeInTheDocument();
  });

  it("states the human confirmation boundary", () => {
    render(<App />);
    expect(screen.getByText(/AI findings will remain suggestions/i)).toBeInTheDocument();
  });
});

function jsonResponse(value: unknown) {
  return new Response(JSON.stringify(value), { status: 200, headers: { "Content-Type": "application/json" } });
}
