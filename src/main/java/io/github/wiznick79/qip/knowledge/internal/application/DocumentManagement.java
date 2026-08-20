package io.github.wiznick79.qip.knowledge.internal.application;

import io.github.wiznick79.qip.knowledge.api.DocumentMediaType;
import io.github.wiznick79.qip.knowledge.api.DocumentNotFoundException;
import io.github.wiznick79.qip.knowledge.api.DocumentSnapshot;
import io.github.wiznick79.qip.knowledge.api.DocumentStatus;
import io.github.wiznick79.qip.knowledge.internal.domain.SourceDocument;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DocumentManagement {

    private static final byte[] PDF_SIGNATURE = {'%', 'P', 'D', 'F', '-'};

    private final DocumentRepository repository;
    private final DocumentStorage storage;
    private final TextExtractor extractor;
    private final DocumentIdGenerator idGenerator;
    private final Clock clock;
    private final long maxUploadBytes;

    public DocumentManagement(
            DocumentRepository repository,
            DocumentStorage storage,
            TextExtractor extractor,
            DocumentIdGenerator idGenerator,
            Clock clock,
            @Value("${qip.documents.max-upload-bytes}") long maxUploadBytes) {
        this.repository = repository;
        this.storage = storage;
        this.extractor = extractor;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.maxUploadBytes = maxUploadBytes;
    }

    public UploadDocumentResult upload(UploadDocumentCommand command) {
        ValidatedUpload upload = validate(command);
        String checksum = sha256(upload.content());
        var duplicate = repository.findByChecksum(checksum);
        if (duplicate.isPresent()) {
            return new UploadDocumentResult(snapshot(duplicate.orElseThrow()), false);
        }

        UUID documentId = idGenerator.nextId();
        String storageKey = storage.store(documentId, upload.content());
        Instant now = Instant.now(clock);
        SourceDocument document = new SourceDocument(
                documentId,
                upload.title(),
                upload.originalFilename(),
                upload.mediaType(),
                upload.content().length,
                checksum,
                storageKey,
                DocumentStatus.UPLOADED,
                null,
                now,
                now);
        try {
            document = repository.save(document);
        } catch (RuntimeException exception) {
            storage.delete(storageKey);
            throw exception;
        }

        return new UploadDocumentResult(extract(document), true);
    }

    public DocumentSnapshot retryExtraction(UUID documentId) {
        SourceDocument document =
                repository.findById(documentId).orElseThrow(() -> new DocumentNotFoundException(documentId));
        return extract(document);
    }

    public DocumentSnapshot getDocument(UUID documentId) {
        return repository
                .findById(documentId)
                .map(this::snapshot)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
    }

    public DocumentPage listDocuments(int page, int size) {
        return repository.findAll(page, size);
    }

    private DocumentSnapshot extract(SourceDocument document) {
        if (document.status() == DocumentStatus.EXTRACTED) {
            return snapshot(document);
        }
        SourceDocument extracting = repository.save(document.startExtraction(Instant.now(clock)));
        try {
            List<ExtractedPage> pages =
                    extractor.extract(storage.read(extracting.storageKey()), extracting.mediaType());
            if (pages.isEmpty()) {
                throw new DocumentExtractionException("Document contains no extractable text");
            }
            SourceDocument extracted = extracting.completeExtraction(Instant.now(clock));
            return snapshot(repository.saveExtraction(extracted, pages));
        } catch (RuntimeException exception) {
            String reason = safeFailureReason(exception);
            SourceDocument failed = extracting.failExtraction(reason, Instant.now(clock));
            return snapshot(repository.save(failed));
        }
    }

    DocumentSnapshot snapshot(SourceDocument document) {
        int pageCount =
                document.status() == DocumentStatus.EXTRACTED ? repository.extractedPageCount(document.id()) : 0;
        return new DocumentSnapshot(
                document.id(),
                document.title(),
                document.originalFilename(),
                document.mediaType(),
                document.sizeBytes(),
                document.checksumSha256(),
                document.status(),
                document.failureReason(),
                pageCount,
                document.uploadedAt(),
                document.updatedAt());
    }

    private ValidatedUpload validate(UploadDocumentCommand command) {
        if (command == null) {
            throw new InvalidDocumentUploadException("Upload is required");
        }
        String title = boundedText(command.title(), "Title", 200);
        String filename = safeFilename(command.originalFilename());
        byte[] content = command.content();
        if (content == null || content.length == 0) {
            throw new InvalidDocumentUploadException("File must not be empty");
        }
        if (content.length > maxUploadBytes) {
            throw new InvalidDocumentUploadException("File exceeds the configured upload size limit");
        }
        DocumentMediaType mediaType = mediaType(command.declaredMediaType());
        validateContent(content, mediaType);
        return new ValidatedUpload(title, filename, mediaType, content);
    }

    private static DocumentMediaType mediaType(String declaredMediaType) {
        String normalized = declaredMediaType == null ? "" : declaredMediaType.split(";", 2)[0].trim();
        if (DocumentMediaType.PDF.value().equalsIgnoreCase(normalized)) {
            return DocumentMediaType.PDF;
        }
        if (DocumentMediaType.PLAIN_TEXT.value().equalsIgnoreCase(normalized)) {
            return DocumentMediaType.PLAIN_TEXT;
        }
        throw new InvalidDocumentUploadException("Only PDF and plain-text documents are accepted");
    }

    private static void validateContent(byte[] content, DocumentMediaType mediaType) {
        if (mediaType == DocumentMediaType.PDF) {
            if (content.length < PDF_SIGNATURE.length) {
                throw new InvalidDocumentUploadException("File content does not match the declared PDF media type");
            }
            for (int index = 0; index < PDF_SIGNATURE.length; index++) {
                if (content[index] != PDF_SIGNATURE[index]) {
                    throw new InvalidDocumentUploadException("File content does not match the declared PDF media type");
                }
            }
            return;
        }
        try {
            String decoded = StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(content))
                    .toString();
            if (decoded.indexOf('\0') >= 0) {
                throw new InvalidDocumentUploadException("Plain-text documents must not contain null characters");
            }
        } catch (CharacterCodingException exception) {
            throw new InvalidDocumentUploadException("Plain-text documents must use valid UTF-8");
        }
    }

    private static String safeFilename(String filename) {
        String value = boundedText(filename, "Filename", 255).replace('\\', '/');
        value = value.substring(value.lastIndexOf('/') + 1).trim();
        if (value.isEmpty()
                || value.equals(".")
                || value.equals("..")
                || value.chars().anyMatch(Character::isISOControl)) {
            throw new InvalidDocumentUploadException("Filename is invalid");
        }
        return value;
    }

    private static String boundedText(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new InvalidDocumentUploadException(label + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new InvalidDocumentUploadException(label + " exceeds " + maxLength + " characters");
        }
        return trimmed;
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String safeFailureReason(RuntimeException exception) {
        if (exception instanceof DocumentExtractionException && exception.getMessage() != null) {
            return truncate(exception.getMessage(), 500);
        }
        return "Document extraction failed";
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private record ValidatedUpload(
            String title, String originalFilename, DocumentMediaType mediaType, byte[] content) {}
}
