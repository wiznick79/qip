import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { InvestigationsPage } from "./InvestigationsPage";

const incident = {
  id: "incident-1", assetId: "asset-1", title: "Synthetic pump leak", description: "Oil near the seal",
  severity: "HIGH", status: "INVESTIGATING", occurredAt: "2026-08-20T09:00:00Z",
  createdAt: "2026-08-20T10:00:00Z", updatedAt: "2026-08-20T10:00:00Z",
};
const closedIncident = {
  ...incident,
  id: "incident-closed",
  title: "Resolved conveyor stop",
  status: "CLOSED",
  occurredAt: "2026-08-19T09:00:00Z",
};
const asset = {
  id: "asset-1", name: "Synthetic pump", type: "MACHINE", externalReference: "SYN-PUMP-1",
  createdAt: "2026-08-20T08:00:00Z",
};
const document = {
  id: "document-1", title: "Synthetic pump manual", originalFilename: "pump.txt", mediaType: "text/plain",
  sizeBytes: 120, checksumSha256: "a".repeat(64), status: "INDEXED", failureReason: null,
  extractedPageCount: 1, uploadedAt: "2026-08-20T10:00:00Z", updatedAt: "2026-08-20T10:00:01Z",
};
const groundedQuestion = {
  id: "question-1", question: "What should be inspected?", selectedDocumentIds: ["document-1"],
  status: "GROUNDED", answer: "Inspect the synthetic hydraulic seal.", modelId: "deterministic-grounded-v1",
  promptVersion: "grounded-answer-v1", retrievedPassageCount: 1, failureReason: null,
  askedAt: "2026-08-20T10:01:00Z", completedAt: "2026-08-20T10:01:01Z",
  citations: [{ passageId: "passage-1", documentId: "document-1", documentTitle: "Synthetic pump manual",
    pageNumber: 1, passageSequence: 0, excerpt: "Inspect the synthetic hydraulic seal.", relevanceScore: 0.88 }],
};

describe("Investigation workspace", () => {
  let findings: Array<Record<string, unknown>>;
  let closed: boolean;
  let incidentStatus: "REPORTED" | "INVESTIGATING" | "RESOLVED";
  let questionResponse: Record<string, unknown>;

  beforeEach(() => {
    findings = [];
    closed = false;
    incidentStatus = "INVESTIGATING";
    questionResponse = groundedQuestion;
    vi.spyOn(globalThis, "fetch").mockImplementation(async (input, init) => {
      const url = String(input);
      if (url.startsWith("/api/assets?")) return json({ items: [asset], page: 0, size: 100, totalElements: 1 });
      if (url.startsWith("/api/incidents?")) return json({ items: [{ ...incident, status: incidentStatus }, closedIncident], page: 0, size: 100, totalElements: 2 });
      if (url.startsWith("/api/documents?")) return json({ items: [document], page: 0, size: 100, totalElements: 1 });
      if (url === "/api/incidents/incident-1" && (!init?.method || init.method === "GET")) {
        return json({ ...incident, status: incidentStatus });
      }
      if (url === "/api/incidents/incident-1/investigations") return json({ id: "investigation-1", incidentId: "incident-1", status: "OPEN", closureSummary: null, closedBy: null, closedAt: null, questions: [], findings: [], createdAt: "2026-08-20T10:00:00Z", updatedAt: "2026-08-20T10:00:00Z" });
      if (url === "/api/investigations/investigation-1/questions" && init?.method === "POST") return json(questionResponse);
      if (url === "/api/investigations/investigation-1/findings" && init?.method === "POST") {
        const body = JSON.parse(String(init.body)) as { sourceQuestionId: string; summary: string };
        findings = [{ id: "finding-1", sourceQuestionId: body.sourceQuestionId, summary: body.summary,
          status: "DRAFT", proposedBy: "wiznick79", proposedAt: "2026-08-20T10:02:00Z",
          reviewedBy: null, reviewRationale: null, reviewedAt: null,
          events: [{ id: "event-1", type: "PROPOSED", actorReference: "wiznick79",
            rationale: null, occurredAt: "2026-08-20T10:02:00Z" }] }];
        return json(findings[0]);
      }
      if (url === "/api/investigations/investigation-1/findings/finding-1/reviews" && init?.method === "POST") {
        const body = JSON.parse(String(init.body)) as { decision: string; rationale: string };
        findings = [{ ...findings[0], status: body.decision, reviewedBy: "wiznick79",
          reviewRationale: body.rationale, reviewedAt: "2026-08-20T10:03:00Z",
          events: [...(findings[0].events as unknown[]), { id: "event-2", type: body.decision,
            actorReference: "wiznick79", rationale: body.rationale,
            occurredAt: "2026-08-20T10:03:00Z" }] }];
        return json(findings[0]);
      }
      if (url === "/api/investigations/investigation-1/closure" && init?.method === "POST") {
        const body = JSON.parse(String(init.body)) as { summary: string };
        closed = true;
        incidentStatus = "RESOLVED";
        return json({ id: "investigation-1", incidentId: "incident-1", status: "CLOSED",
          closureSummary: body.summary, closedBy: "wiznick79", closedAt: "2026-08-20T10:04:00Z",
          questions: [groundedQuestion], findings, createdAt: "2026-08-20T10:00:00Z", updatedAt: "2026-08-20T10:04:00Z" });
      }
      if (url === "/api/incidents/incident-1/status" && init?.method === "PATCH") {
        const body = JSON.parse(String(init.body)) as { status: typeof incidentStatus };
        incidentStatus = body.status;
        return json({ ...incident, status: incidentStatus, updatedAt: "2026-08-20T10:05:00Z" });
      }
      if (url === "/api/investigations/investigation-1") return json({ id: "investigation-1", incidentId: "incident-1", status: closed ? "CLOSED" : "OPEN", closureSummary: closed ? "The confirmed finding closes this case." : null, closedBy: closed ? "wiznick79" : null, closedAt: closed ? "2026-08-20T10:04:00Z" : null, questions: [questionResponse], findings, createdAt: "2026-08-20T10:00:00Z", updatedAt: "2026-08-20T10:01:01Z" });
      return new Response(null, { status: 404 });
    });
  });

  afterEach(() => vi.restoreAllMocks());

  it("opens a case, asks against selected documents, and renders passage provenance", async () => {
    render(<InvestigationsPage />);
    fireEvent.click(await screen.findByRole("button", { name: /Select Synthetic pump leak incident/ }));
    fireEvent.click(screen.getByRole("button", { name: "Open investigation" }));
    expect(await screen.findByText("No questions yet")).toBeInTheDocument();
    expect(statusUpdateCalls()).toHaveLength(0);

    fireEvent.click(screen.getByLabelText(/Synthetic pump manual/i));
    fireEvent.change(screen.getByLabelText("Question"), { target: { value: "What should be inspected?" } });
    fireEvent.click(screen.getByRole("button", { name: "Ask with evidence" }));

    expect(await screen.findAllByText("Inspect the synthetic hydraulic seal.")).toHaveLength(2);
    expect(screen.getByText("GROUNDED")).toBeInTheDocument();
    fireEvent.click(screen.getByText(/Synthetic pump manual · page 1/));
    expect(screen.getByText("Passage 1 · relevance 0.880")).toBeInTheDocument();
    await waitFor(() => expect(screen.getByLabelText("Question")).toHaveValue(""));
  });

  it("identifies the model whose response was rejected", async () => {
    questionResponse = {
      ...groundedQuestion,
      status: "TECHNICAL_FAILURE",
      answer: null,
      citations: [],
      modelId: "ollama:qwen3-coder:30b",
      failureReason: "Answer provider returned conflicting response blocks",
    };
    render(<InvestigationsPage />);
    fireEvent.click(await screen.findByRole("button", { name: /Select Synthetic pump leak incident/ }));
    fireEvent.click(screen.getByRole("button", { name: "Open investigation" }));
    await screen.findByText("No questions yet");
    fireEvent.change(screen.getByLabelText("Question"), { target: { value: "Is it fixable?" } });
    fireEvent.click(screen.getByRole("button", { name: "Ask with evidence" }));

    expect(await screen.findByText("Answer provider returned conflicting response blocks")).toBeInTheDocument();
    expect(screen.getByText(/Response rejected from ollama:qwen3-coder:30b/)).toBeInTheDocument();
  });

  it("moves a reported incident to investigating before opening its investigation", async () => {
    incidentStatus = "REPORTED";
    render(<InvestigationsPage />);
    fireEvent.click(await screen.findByRole("button", { name: /Select Synthetic pump leak incident/ }));
    fireEvent.click(screen.getByRole("button", { name: "Open investigation" }));

    expect(await screen.findByText("No questions yet")).toBeInTheDocument();
    expect(screen.getByText("HIGH · INVESTIGATING")).toBeInTheDocument();
    expect(statusUpdateCalls()).toHaveLength(1);
    expect(statusUpdateCalls()[0]?.[1]).toEqual(expect.objectContaining({
      method: "PATCH",
      body: JSON.stringify({ status: "INVESTIGATING" }),
    }));
  });
  it("requires explicit proposal and review before a finding is confirmed", async () => {
    render(<InvestigationsPage />);
    fireEvent.click(await screen.findByRole("button", { name: /Select Synthetic pump leak incident/ }));
    fireEvent.click(screen.getByRole("button", { name: "Open investigation" }));
    await screen.findByText("No questions yet");
    fireEvent.change(screen.getByLabelText("Question"), { target: { value: "What should be inspected?" } });
    fireEvent.click(screen.getByRole("button", { name: "Ask with evidence" }));

    fireEvent.click(await screen.findByRole("button", { name: "Propose as finding" }));
    expect(screen.getByLabelText("Finding summary")).toHaveValue("Inspect the synthetic hydraulic seal.");
    expect(screen.queryByLabelText("Proposed by")).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Create draft finding" }));

    expect(await screen.findByText("DRAFT")).toBeInTheDocument();
    expect(screen.getByText(/Audit history · 1 event/)).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Rationale"), { target: { value: "The cited source supports inspection." } });
    fireEvent.click(screen.getByRole("button", { name: "Record review decision" }));

    expect(await screen.findAllByText("CONFIRMED")).toHaveLength(2);
    expect(screen.getByText(/CONFIRMED by wiznick79/)).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Record review decision" })).not.toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("Closure summary"), { target: { value: "The confirmed finding closes this case." } });
    fireEvent.click(screen.getByRole("button", { name: "Close investigation" }));

    expect(await screen.findByText("Case closure")).toBeInTheDocument();
    expect(screen.getByText("The confirmed finding closes this case.")).toBeInTheDocument();
    expect(screen.queryByLabelText("Question")).not.toBeInTheDocument();
    expect(await screen.findByText(/Incident status:/)).toHaveTextContent("RESOLVED");
    expect(statusUpdateCalls()).toHaveLength(0);
  });

  it("does not offer finding review to an investigator without the reviewer role", async () => {
    render(<InvestigationsPage session={sessionWithRoles("INVESTIGATOR")} />);
    fireEvent.click(await screen.findByRole("button", { name: /Select Synthetic pump leak incident/ }));
    fireEvent.click(screen.getByRole("button", { name: "Open investigation" }));
    await screen.findByText("No questions yet");
    fireEvent.change(screen.getByLabelText("Question"), { target: { value: "What should be inspected?" } });
    fireEvent.click(screen.getByRole("button", { name: "Ask with evidence" }));
    fireEvent.click(await screen.findByRole("button", { name: "Propose as finding" }));
    fireEvent.click(screen.getByRole("button", { name: "Create draft finding" }));

    expect(await screen.findByText("You do not have permission to review this finding.")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Record review decision" })).not.toBeInTheDocument();
  });

  it("does not offer investigation closure to a reviewer without the investigator role", async () => {
    render(<InvestigationsPage session={sessionWithRoles("REVIEWER")} />);
    fireEvent.click(await screen.findByRole("button", { name: /Select Synthetic pump leak incident/ }));
    fireEvent.click(screen.getByRole("button", { name: "Open investigation" }));
    await screen.findByText("No questions yet");
    fireEvent.change(screen.getByLabelText("Question"), { target: { value: "What should be inspected?" } });
    fireEvent.click(screen.getByRole("button", { name: "Ask with evidence" }));
    fireEvent.click(await screen.findByRole("button", { name: "Propose as finding" }));
    fireEvent.click(screen.getByRole("button", { name: "Create draft finding" }));
    await screen.findByText("DRAFT");
    fireEvent.change(screen.getByLabelText("Rationale"), { target: { value: "The cited source supports inspection." } });
    fireEvent.click(screen.getByRole("button", { name: "Record review decision" }));

    expect(await screen.findByText("You do not have permission to close this investigation.")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Close investigation" })).not.toBeInTheDocument();
  });
  it("separates recent active incidents from searchable history", async () => {
    const viewAll = vi.fn();
    render(<InvestigationsPage onViewAllIncidents={viewAll} />);

    expect(await screen.findByRole("button", { name: /Select Synthetic pump leak incident/ })).toBeInTheDocument();
    expect(screen.queryByText("Resolved conveyor stop")).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("tab", { name: "History" }));
    expect(screen.getByText("Resolved conveyor stop")).toBeInTheDocument();
    fireEvent.change(screen.getByRole("searchbox", { name: "Search incidents" }), { target: { value: "missing" } });
    expect(screen.getByText("No matching history incidents.")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "View all incidents →" }));
    expect(viewAll).toHaveBeenCalledOnce();
  });
});

function sessionWithRoles(...roles: string[]) {
  return {
    authenticated: true,
    username: "wiznick79",
    roles,
    csrfHeaderName: "X-CSRF-TOKEN",
    csrfToken: "test-csrf-token",
  };
}
function statusUpdateCalls() {
  return vi.mocked(globalThis.fetch).mock.calls.filter(([input, init]) =>
    String(input) === "/api/incidents/incident-1/status" && init?.method === "PATCH");
}
function json(value: unknown) {
  return new Response(JSON.stringify(value), { status: 200, headers: { "Content-Type": "application/json" } });
}
