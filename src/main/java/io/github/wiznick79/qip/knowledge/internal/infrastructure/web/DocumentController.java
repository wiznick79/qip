package io.github.wiznick79.qip.knowledge.internal.infrastructure.web;

import io.github.wiznick79.qip.knowledge.internal.application.DocumentManagement;
import io.github.wiznick79.qip.knowledge.internal.application.UploadDocumentCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/documents")
class DocumentController {

    private final DocumentManagement documents;

    DocumentController(DocumentManagement documents) {
        this.documents = documents;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<DocumentResponse> upload(@RequestPart("title") String title, @RequestPart("file") MultipartFile file)
            throws IOException {
        var result = documents.upload(
                new UploadDocumentCommand(title, file.getOriginalFilename(), file.getContentType(), file.getBytes()));
        var response = DocumentResponse.from(result.document());
        if (!result.created()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.created(URI.create("/api/documents/" + response.id()))
                .body(response);
    }

    @GetMapping("/{documentId}")
    DocumentResponse get(@PathVariable UUID documentId) {
        return DocumentResponse.from(documents.getDocument(documentId));
    }

    @GetMapping
    DocumentPageResponse list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return DocumentPageResponse.from(documents.listDocuments(page, size));
    }

    @GetMapping("/{documentId}/status")
    DocumentStatusResponse status(@PathVariable UUID documentId) {
        return DocumentStatusResponse.from(documents.getDocument(documentId));
    }

    @PostMapping("/{documentId}/extraction")
    DocumentStatusResponse retryExtraction(@PathVariable UUID documentId) {
        return DocumentStatusResponse.from(documents.retryExtraction(documentId));
    }
}
