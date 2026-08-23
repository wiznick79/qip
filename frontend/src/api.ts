import type {
  Asset,
  AssetType,
  Incident,
  IncidentSeverity,
  IncidentStatus,
  Observation,
  EvidenceItem,
  EvidenceType,
  Investigation,
  Finding,
  FindingStatus,
  Page,
  ProblemDetails,
  SourceDocument,
  QuestionAnswer,
  UserSession,
} from "./types";

let csrfHeaderName: string | null = null;
let csrfToken: string | null = null;

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly problem?: ProblemDetails,
  ) {
    super(message);
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const method = init?.method?.toUpperCase() ?? "GET";
  const stateChanging = !["GET", "HEAD", "OPTIONS"].includes(method);
  let headers = init?.body instanceof FormData
    ? init.headers
    : { "Content-Type": "application/json", ...init?.headers };
  if (stateChanging && csrfHeaderName && csrfToken) {
    const csrfHeaders = new Headers(headers);
    csrfHeaders.set(csrfHeaderName, csrfToken);
    headers = csrfHeaders;
  }
  const response = await fetch(path, {
    ...init,
    credentials: "same-origin",
    headers,
  });
  if (!response.ok) {
    const problem = (await response.json().catch(() => undefined)) as ProblemDetails | undefined;
    const fieldMessage = problem?.errors ? Object.values(problem.errors)[0] : undefined;
    throw new ApiError(
      fieldMessage ?? problem?.detail ?? "The request could not be completed.",
      response.status,
      problem,
    );
  }
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

export const api = {
  getSession: async () => {
    const session = await request<UserSession>("/api/session");
    csrfHeaderName = session.csrfHeaderName;
    csrfToken = session.csrfToken;
    return session;
  },
  login: async (username: string, password: string) => {
    const form = new URLSearchParams({ username, password });
    await request<void>("/api/session/login", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: form,
    });
    return api.getSession();
  },
  logout: async () => {
    await request<void>("/api/session/logout", { method: "POST" });
    return api.getSession();
  },
  listAssets: () => request<Page<Asset>>("/api/assets?page=0&size=100"),
  createAsset: (input: { name: string; type: AssetType; externalReference: string | null }) =>
    request<Asset>("/api/assets", { method: "POST", body: JSON.stringify(input) }),
  listIncidents: (status?: IncidentStatus, page = 0, size = 100) => {
    const query = new URLSearchParams({ page: String(page), size: String(size) });
    if (status) query.set("status", status);
    return request<Page<Incident>>(`/api/incidents?${query}`);
  },
  createIncident: (input: {
    assetId: string;
    title: string;
    description: string | null;
    severity: IncidentSeverity;
    occurredAt: string;
  }) => request<Incident>("/api/incidents", { method: "POST", body: JSON.stringify(input) }),
  getIncident: (incidentId: string) => request<Incident>(`/api/incidents/${encodeURIComponent(incidentId)}`),
  updateIncidentStatus: (incidentId: string, status: IncidentStatus) =>
    request<Incident>(`/api/incidents/${encodeURIComponent(incidentId)}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status }),
    }),
  listObservations: (incidentId: string, page = 0, size = 20) =>
    request<Page<Observation>>(`/api/incidents/${encodeURIComponent(incidentId)}/observations?page=${page}&size=${size}`),
  appendObservation: (incidentId: string, input: { text: string; observedAt: string }) =>
    request<Observation>(`/api/incidents/${encodeURIComponent(incidentId)}/observations`, {
      method: "POST",
      body: JSON.stringify(input),
    }),
  listEvidence: (incidentId: string, page = 0, size = 20) =>
    request<Page<EvidenceItem>>(`/api/incidents/${encodeURIComponent(incidentId)}/evidence?page=${page}&size=${size}`),
  appendEvidence: (incidentId: string, input: {
    type: EvidenceType;
    summary: string;
    sourceReference: string;
    eventAt: string;
  }) => request<EvidenceItem>(`/api/incidents/${encodeURIComponent(incidentId)}/evidence`, {
    method: "POST",
    body: JSON.stringify(input),
  }),
  listDocuments: () => request<Page<SourceDocument>>("/api/documents?page=0&size=100"),
  uploadDocument: (title: string, file: File) => {
    const form = new FormData();
    form.append("title", title);
    form.append("file", file);
    return request<SourceDocument>("/api/documents", { method: "POST", body: form });
  },
  createInvestigation: (incidentId: string) =>
    request<Investigation>(`/api/incidents/${encodeURIComponent(incidentId)}/investigations`, { method: "POST" }),
  getInvestigation: (investigationId: string) =>
    request<Investigation>(`/api/investigations/${investigationId}`),
  askQuestion: (investigationId: string, question: string, documentIds: string[]) =>
    request<QuestionAnswer>(`/api/investigations/${investigationId}/questions`, {
      method: "POST",
      body: JSON.stringify({ question, documentIds }),
    }),
  proposeFinding: (
    investigationId: string,
    input: { sourceQuestionId: string; summary: string },
  ) => request<Finding>(`/api/investigations/${investigationId}/findings`, {
    method: "POST",
    body: JSON.stringify(input),
  }),
  reviewFinding: (
    investigationId: string,
    findingId: string,
    input: { decision: Exclude<FindingStatus, "DRAFT">; rationale: string },
  ) => request<Finding>(`/api/investigations/${investigationId}/findings/${findingId}/reviews`, {
    method: "POST",
    body: JSON.stringify(input),
  }),
  closeInvestigation: (investigationId: string, input: { summary: string }) =>
    request<Investigation>(`/api/investigations/${investigationId}/closure`, {
      method: "POST",
      body: JSON.stringify(input),
    }),
};
