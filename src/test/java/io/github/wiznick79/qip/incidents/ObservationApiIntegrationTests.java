package io.github.wiznick79.qip.incidents;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(properties = "qip.security.enabled=false")
class ObservationApiIntegrationTests {

    private static final DockerImageName PGVECTOR_IMAGE =
            DockerImageName.parse("pgvector/pgvector:0.8.6-pg17-bookworm").asCompatibleSubstituteFor("postgres");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer(PGVECTOR_IMAGE);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void clearData() {
        jdbcClient.sql("DELETE FROM incident_observations").update();
        jdbcClient.sql("DELETE FROM incidents").update();
        jdbcClient.sql("DELETE FROM assets").update();
    }

    @Test
    void appendsAndListsAttributedObservation() throws Exception {
        String incidentId = createIncident();
        String observedAt = "2026-08-17T10:00:00Z";

        String body = appendObservation(incidentId, "  Oil visible beneath the pump.  ", "investigator-17", observedAt);
        String observationId = JsonPath.read(body, "$.id");

        mockMvc.perform(get("/api/incidents/{incidentId}/observations", incidentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(observationId))
                .andExpect(jsonPath("$.items[0].incidentId").value(incidentId))
                .andExpect(jsonPath("$.items[0].text").value("Oil visible beneath the pump."))
                .andExpect(jsonPath("$.items[0].authorReference").value("investigator-17"))
                .andExpect(jsonPath("$.items[0].observedAt").value(observedAt))
                .andExpect(jsonPath("$.items[0].recordedAt").isNotEmpty())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void returnsTimelineInDeterministicOrderWithBoundedPagination() throws Exception {
        String incidentId = createIncident();
        appendObservation(incidentId, "Later observation", "investigator-17", "2026-08-17T11:00:00Z");
        appendObservation(incidentId, "Earlier observation", "investigator-17", "2026-08-17T09:00:00Z");

        mockMvc.perform(get("/api/incidents/{incidentId}/observations", incidentId)
                        .queryParam("page", "0")
                        .queryParam("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].text").value("Earlier observation"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void rejectsObservationForMissingIncident() throws Exception {
        String missingIncidentId = "00000000-0000-0000-0000-000000000999";

        mockMvc.perform(post("/api/incidents/{incidentId}/observations", missingIncidentId)
                        .with(user("investigator-17"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(observationRequest("Gauge read zero", "investigator-17", "2026-08-17T10:00:00Z")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Incident not found"))
                .andExpect(jsonPath("$.incidentId").value(missingIncidentId));
    }

    @Test
    void rejectsFutureObservationTime() throws Exception {
        String incidentId = createIncident();
        String futureTime = Instant.now().plusSeconds(3600).toString();

        mockMvc.perform(post("/api/incidents/{incidentId}/observations", incidentId)
                        .with(user("investigator-17"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(observationRequest("Gauge read zero", "investigator-17", futureTime)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid observation time"))
                .andExpect(jsonPath("$.observedAt").value(futureTime));
    }

    @Test
    void validatesObservationRequest() throws Exception {
        String incidentId = createIncident();

        mockMvc.perform(post("/api/incidents/{incidentId}/observations", incidentId)
                        .with(user("investigator-17"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":" ","authorReference":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.text").isNotEmpty())
                .andExpect(jsonPath("$.errors.observedAt").isNotEmpty());
    }

    private String createIncident() throws Exception {
        String assetBody = mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Synthetic Pump D","type":"MACHINE"}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String assetId = JsonPath.read(assetBody, "$.id");

        String incidentBody = mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assetId":"%s",
                                  "title":"Seal leakage",
                                  "severity":"MEDIUM",
                                  "occurredAt":"2026-08-17T08:00:00Z"
                                }
                                """.formatted(assetId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(incidentBody, "$.id");
    }

    private String appendObservation(String incidentId, String text, String authorReference, String observedAt)
            throws Exception {
        return mockMvc.perform(post("/api/incidents/{incidentId}/observations", incidentId)
                        .with(user("investigator-17"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(observationRequest(text, authorReference, observedAt)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String observationRequest(String text, String authorReference, String observedAt) {
        return """
                {
                  "text":"%s",
                  "authorReference":"%s",
                  "observedAt":"%s"
                }
                """.formatted(text, authorReference, observedAt);
    }
}
