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

export type ProblemDetails = {
  title?: string;
  detail?: string;
  status?: number;
  errors?: Record<string, string>;
};
