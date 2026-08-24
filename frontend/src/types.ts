export type Page<T> = {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
};

export type UserSession = {
  authenticated: boolean;
  username: string | null;
  roles: string[];
  csrfHeaderName: string;
  csrfToken: string;
};

export type HealthStatus = {
  status: string;
};

export type MetricMeasurement = {
  statistic: "COUNT" | "TOTAL" | "TOTAL_TIME" | "MAX" | "VALUE" | string;
  value: number;
};

export type MetricSnapshot = {
  name: string;
  description: string | null;
  baseUnit: string | null;
  measurements: MetricMeasurement[];
};

export type AssetType = "MACHINE" | "PRODUCTION_LINE" | "TOOL" | "OTHER";

export type Asset = {
  id: string;
  name: string;
  type: AssetType;
  externalReference: string | null;
  createdAt: string;
};

export type IncidentSeverity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type IncidentStatus = "REPORTED" | "INVESTIGATING" | "RESOLVED" | "CLOSED";

export type Incident = {
  id: string;
  assetId: string;
  title: string;
  description: string | null;
  severity: IncidentSeverity;
  status: IncidentStatus;
  occurredAt: string;
  createdAt: string;
  updatedAt: string;
};

export type Observation = {
  id: string;
  incidentId: string;
  text: string;
  authorReference: string;
  observedAt: string;
  recordedAt: string;
};

export type EvidenceType = "MEASUREMENT" | "DOCUMENT" | "IMAGE" | "LOG_ENTRY" | "PHYSICAL_ITEM" | "TEST_RESULT" | "OTHER";
export type EvidenceProvenance = "HUMAN_ENTERED" | "IMPORTED" | "RETRIEVED" | "MODEL_GENERATED";

export type EvidenceItem = {
  id: string;
  incidentId: string;
  type: EvidenceType;
  summary: string;
  sourceReference: string;
  eventAt: string;
  provenance: EvidenceProvenance;
  submittedBy: string;
  recordedAt: string;
};

export type DocumentStatus =
  | "UPLOADED"
  | "EXTRACTING"
  | "EXTRACTED"
  | "EXTRACTION_FAILED"
  | "INDEXING"
  | "INDEXED"
  | "INDEXING_FAILED";

export type SourceDocument = {
  id: string;
  title: string;
  originalFilename: string;
  mediaType: "application/pdf" | "text/plain";
  sizeBytes: number;
  checksumSha256: string;
  status: DocumentStatus;
  failureReason: string | null;
  extractedPageCount: number;
  uploadedAt: string;
  updatedAt: string;
};

export type AnswerStatus = "PROCESSING" | "GROUNDED" | "INSUFFICIENT_EVIDENCE" | "TECHNICAL_FAILURE";

export type Citation = {
  passageId: string;
  documentId: string;
  documentTitle: string;
  pageNumber: number;
  passageSequence: number;
  excerpt: string;
  relevanceScore: number;
};

export type QuestionAnswer = {
  id: string;
  question: string;
  selectedDocumentIds: string[];
  status: AnswerStatus;
  answer: string | null;
  citations: Citation[];
  modelId: string | null;
  promptVersion: string;
  retrievedPassageCount: number;
  failureReason: string | null;
  askedAt: string;
  completedAt: string | null;
};

export type FindingStatus = "DRAFT" | "CONFIRMED" | "REJECTED";
export type FindingEventType = "PROPOSED" | "CONFIRMED" | "REJECTED";

export type FindingReviewEvent = {
  id: string;
  type: FindingEventType;
  actorReference: string;
  rationale: string | null;
  occurredAt: string;
};

export type Finding = {
  id: string;
  sourceQuestionId: string;
  summary: string;
  status: FindingStatus;
  proposedBy: string;
  proposedAt: string;
  reviewedBy: string | null;
  reviewRationale: string | null;
  reviewedAt: string | null;
  events: FindingReviewEvent[];
};

export type Investigation = {
  id: string;
  incidentId: string;
  status: "OPEN" | "CLOSED";
  closureSummary: string | null;
  closedBy: string | null;
  closedAt: string | null;
  questions: QuestionAnswer[];
  findings: Finding[];
  createdAt: string;
  updatedAt: string;
};

export type ProblemDetails = {
  title?: string;
  detail?: string;
  status?: number;
  errors?: Record<string, string>;
};
