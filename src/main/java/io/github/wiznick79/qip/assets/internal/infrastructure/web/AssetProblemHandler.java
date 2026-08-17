package io.github.wiznick79.qip.assets.internal.infrastructure.web;

import io.github.wiznick79.qip.assets.api.AssetNotFoundException;
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
@RestControllerAdvice(basePackageClasses = AssetController.class)
class AssetProblemHandler {

    @ExceptionHandler(AssetNotFoundException.class)
    ProblemDetail assetNotFound(AssetNotFoundException exception) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "No asset exists with the supplied ID.");
        problem.setType(URI.create("https://github.com/wiznick79/qip/problems/asset-not-found"));
        problem.setTitle("Asset not found");
        problem.setProperty("assetId", exception.assetId());
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
