export type Page<T> = {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
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

export type Investigation = {
  id: string;
  incidentId: string;
  questions: QuestionAnswer[];
  createdAt: string;
  updatedAt: string;
};

export type ProblemDetails = {
  title?: string;
  detail?: string;
  status?: number;
  errors?: Record<string, string>;
};
