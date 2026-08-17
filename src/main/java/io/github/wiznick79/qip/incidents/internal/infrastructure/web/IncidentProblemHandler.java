package io.github.wiznick79.qip.incidents.internal.infrastructure.web;

import io.github.wiznick79.qip.assets.api.AssetNotFoundException;
import io.github.wiznick79.qip.incidents.api.IncidentNotFoundException;
import io.github.wiznick79.qip.incidents.api.InvalidIncidentTransitionException;
import io.github.wiznick79.qip.incidents.internal.application.InvalidIncidentSearchRangeException;
import io.github.wiznick79.qip.incidents.internal.domain.InvalidObservationTimeException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackageClasses = IncidentController.class)
class IncidentProblemHandler {

    @ExceptionHandler(IncidentNotFoundException.class)
    ProblemDetail incidentNotFound(IncidentNotFoundException exception) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "No incident exists with the supplied ID.");
        problem.setType(URI.create("https://github.com/wiznick79/qip/problems/incident-not-found"));
        problem.setTitle("Incident not found");
        problem.setProperty("incidentId", exception.incidentId());
        return problem;
    }

    @ExceptionHandler(AssetNotFoundException.class)
    ProblemDetail assetNotFound(AssetNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_CONTENT, "The supplied asset does not exist.");
        problem.setType(URI.create("https://github.com/wiznick79/qip/problems/incident-asset-not-found"));
        problem.setTitle("Incident asset not found");
        problem.setProperty("assetId", exception.assetId());
        return problem;
    }

    @ExceptionHandler(InvalidIncidentTransitionException.class)
    ProblemDetail invalidTransition(InvalidIncidentTransitionException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "The requested incident status transition is not allowed.");
        problem.setType(URI.create("https://github.com/wiznick79/qip/problems/invalid-incident-transition"));
        problem.setTitle("Invalid incident transition");
        problem.setProperty("currentStatus", exception.currentStatus());
        problem.setProperty("requestedStatus", exception.requestedStatus());
        return problem;
    }

    @ExceptionHandler(InvalidIncidentSearchRangeException.class)
    ProblemDetail invalidSearchRange(InvalidIncidentSearchRangeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "The 'from' instant must be before the 'to' instant.");
        problem.setType(URI.create("https://github.com/wiznick79/qip/problems/invalid-incident-search-range"));
        problem.setTitle("Invalid incident search range");
        problem.setProperty("from", exception.from());
        problem.setProperty("to", exception.to());
        return problem;
    }

    @ExceptionHandler(InvalidObservationTimeException.class)
    ProblemDetail invalidObservationTime(InvalidObservationTimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "The observation time must not be later than its recording time.");
        problem.setType(URI.create("https://github.com/wiznick79/qip/problems/invalid-observation-time"));
        problem.setTitle("Invalid observation time");
        problem.setProperty("observedAt", exception.observedAt());
        problem.setProperty("recordedAt", exception.recordedAt());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail requestValidationFailed(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception
                .getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.putIfAbsent(
                        error.getField(),
                        error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()));

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more request fields are invalid.");
        problem.setType(URI.create("https://github.com/wiznick79/qip/problems/request-validation"));
        problem.setTitle("Request validation failed");
        problem.setProperty("errors", errors);
        return problem;
    }
}
