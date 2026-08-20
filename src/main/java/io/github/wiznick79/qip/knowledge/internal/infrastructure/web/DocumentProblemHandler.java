package io.github.wiznick79.qip.knowledge.internal.infrastructure.web;

import io.github.wiznick79.qip.knowledge.api.DocumentNotFoundException;
import io.github.wiznick79.qip.knowledge.internal.application.InvalidDocumentUploadException;
import io.github.wiznick79.qip.knowledge.internal.domain.InvalidDocumentStateException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice(basePackageClasses = DocumentController.class)
class DocumentProblemHandler {

    @ExceptionHandler(DocumentNotFoundException.class)
    ProblemDetail documentNotFound(DocumentNotFoundException exception) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "No document exists with the supplied ID.");
        problem.setType(URI.create("https://github.com/wiznick79/qip/problems/document-not-found"));
        problem.setTitle("Document not found");
        problem.setProperty("documentId", exception.documentId());
        return problem;
    }

    @ExceptionHandler(InvalidDocumentUploadException.class)
    ProblemDetail invalidUpload(InvalidDocumentUploadException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setType(URI.create("https://github.com/wiznick79/qip/problems/invalid-document-upload"));
        problem.setTitle("Invalid document upload");
        return problem;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail uploadTooLarge() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONTENT_TOO_LARGE, "File exceeds the configured upload size limit.");
        problem.setType(URI.create("https://github.com/wiznick79/qip/problems/document-upload-too-large"));
        problem.setTitle("Document upload too large");
        return problem;
    }

    @ExceptionHandler(InvalidDocumentStateException.class)
    ProblemDetail invalidState(InvalidDocumentStateException exception) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Document cannot enter the requested state.");
        problem.setType(URI.create("https://github.com/wiznick79/qip/problems/invalid-document-state"));
        problem.setTitle("Invalid document state");
        problem.setProperty("currentStatus", exception.currentStatus());
        return problem;
    }
}
