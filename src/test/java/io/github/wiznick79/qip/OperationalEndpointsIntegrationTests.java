package io.github.wiznick79.qip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
@AutoConfigureMockMvc
@SpringBootTest(properties = "qip.security.enabled=false")
class OperationalEndpointsIntegrationTests {

    private static final DockerImageName PGVECTOR_IMAGE =
            DockerImageName.parse("pgvector/pgvector:0.8.6-pg17-bookworm").asCompatibleSubstituteFor("postgres");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer(PGVECTOR_IMAGE);

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesOnlySafeHealthInformationAndAvailabilityProbes() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/info")).andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.names",
                        org.hamcrest.Matchers.hasItems(
                                "qip.knowledge.ingestion",
                                "qip.knowledge.retrieval",
                                "qip.investigations.model",
                                "qip.investigations.answers")));
        mockMvc.perform(get("/actuator/metrics/qip.knowledge.ingestion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements").isArray());
        mockMvc.perform(get("/actuator/metrics/qip.knowledge.retrieval"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements").isArray());
        mockMvc.perform(get("/actuator/metrics/qip.investigations.model"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements").isArray());
        mockMvc.perform(get("/actuator/metrics/qip.investigations.answers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements").isArray());
    }

    @Test
    void propagatesSafeCorrelationIdsAndReplacesInvalidValues(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/actuator/health").header(CorrelationIdFilter.HEADER, "demo-request-17"))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.HEADER, "demo-request-17"));

        mockMvc.perform(get("/actuator/health").header(CorrelationIdFilter.HEADER, "unsafe value"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                                CorrelationIdFilter.HEADER,
                                org.hamcrest.Matchers.matchesPattern(
                                        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")));
        assertThat(output).contains("\"correlationId\":\"demo-request-17\"");
    }

    @Test
    void publishesOpenApiAndSwaggerUi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Quality Investigation Platform API"))
                .andExpect(jsonPath("$.paths['/api/assets']").exists())
                .andExpect(jsonPath("$.paths['/api/documents']").exists())
                .andExpect(jsonPath("$.paths['/api/investigations/{investigationId}/questions']")
                        .exists());
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("swagger-ui")));
    }
}
