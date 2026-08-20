package io.github.wiznick79.qip.investigations.internal.infrastructure.web;

import io.github.wiznick79.qip.incidents.api.IncidentNotFoundException;
import io.github.wiznick79.qip.investigations.api.FindingNotFoundException;
import io.github.wiznick79.qip.investigations.api.InvestigationNotFoundException;
import io.github.wiznick79.qip.investigations.internal.application.InvalidFindingException;
import io.github.wiznick79.qip.investigations.internal.application.InvalidQuestionException;
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
@RestControllerAdvice(basePackageClasses = InvestigationController.class)
class InvestigationProblemHandler {

    @ExceptionHandler(IncidentNotFoundException.class)
    ProblemDetail incidentNotFound(IncidentNotFoundException exception) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "No incident exists with the supplied ID.");
        problem.setType(URI.create("https://github.com/wiznick79/qip/problems/incident-not-found"));
        problem.setTitle("Incident not found");
        problem.setProperty("incidentId", exception.incidentId());
        return problem;
    }

    @ExceptionHandler(InvestigationNotFoundException.class)
    ProblemDetail investigationNotFound(InvestigationNotFoundException exception) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "No investigation exists with the supplied ID.");
        problem.setType(URI.create("https://github.com/wiznick79/qip/problems/investigation-not-found"));
        problem.setTitle("Investigation not found");
        problem.setProperty("investigationId", exception.investigationId());
        return problem;
    }

    @ExceptionHandler(FindingNotFoundException.class)
    ProblemDetail findingNotFound(FindingNotFoundException exception) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "No finding exists with the supplied ID.");
        problem.setType(URI.create("https://github.com/wiznick79/qip/problems/finding-not-found"));
        problem.setTitle("Finding not found");
        problem.setProperty("findingId", exception.findingId());
        return problem;
    }

    @ExceptionHandler(InvalidFindingException.class)
    ProblemDetail invalidFinding(InvalidFindingException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setType(URI.create("https://github.com/wiznick79/qip/problems/invalid-finding"));
        problem.setTitle("Finding action rejected");
        return problem;
    }

    @ExceptionHandler(InvalidQuestionException.class)
    ProblemDetail invalidQuestion(InvalidQuestionException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setType(URI.create("https://github.com/wiznick79/qip/problems/invalid-question"));
        problem.setTitle("Invalid question");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validationFailed(MethodArgumentNotValidException exception) {
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
