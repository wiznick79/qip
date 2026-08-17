package io.github.wiznick79.qip.incidents;

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
@SpringBootTest
class EvidenceApiIntegrationTests {

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
        jdbcClient.sql("DELETE FROM incident_evidence").update();
        jdbcClient.sql("DELETE FROM incident_observations").update();
        jdbcClient.sql("DELETE FROM incidents").update();
        jdbcClient.sql("DELETE FROM assets").update();
    }

    @Test
    void appendsAndListsTypedSourceAttributedEvidence() throws Exception {
        String incidentId = createIncident();
        String eventAt = "2026-08-17T10:00:00Z";

        String body = appendEvidence(
                incidentId,
                "MEASUREMENT",
                "  Pressure measured at 0 bar.  ",
                "  sensor:pressure-gauge-07  ",
                eventAt,
                "investigator-17");
        String evidenceId = JsonPath.read(body, "$.id");

        mockMvc.perform(get("/api/incidents/{incidentId}/evidence", incidentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(evidenceId))
                .andExpect(jsonPath("$.items[0].incidentId").value(incidentId))
                .andExpect(jsonPath("$.items[0].type").value("MEASUREMENT"))
                .andExpect(jsonPath("$.items[0].summary").value("Pressure measured at 0 bar."))
                .andExpect(jsonPath("$.items[0].sourceReference").value("sensor:pressure-gauge-07"))
                .andExpect(jsonPath("$.items[0].eventAt").value(eventAt))
                .andExpect(jsonPath("$.items[0].provenance").value("HUMAN_ENTERED"))
                .andExpect(jsonPath("$.items[0].submittedBy").value("investigator-17"))
                .andExpect(jsonPath("$.items[0].recordedAt").isNotEmpty())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void returnsEvidenceTimelineInDeterministicOrderWithBoundedPagination() throws Exception {
        String incidentId = createIncident();
        appendEvidence(
                incidentId,
                "LOG_ENTRY",
                "Later log entry",
                "log:controller-09:124",
                "2026-08-17T11:00:00Z",
                "investigator-17");
        appendEvidence(
                incidentId, "IMAGE", "Earlier seal image", "image:seal-001", "2026-08-17T09:00:00Z", "investigator-17");

        mockMvc.perform(get("/api/incidents/{incidentId}/evidence", incidentId)
                        .queryParam("page", "0")
                        .queryParam("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].summary").value("Earlier seal image"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void rejectsEvidenceForMissingIncident() throws Exception {
        String missingIncidentId = "00000000-0000-0000-0000-000000000999";

        mockMvc.perform(post("/api/incidents/{incidentId}/evidence", missingIncidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evidenceRequest(
                                "MEASUREMENT",
                                "Pressure measured at 0 bar.",
                                "sensor:pressure-gauge-07",
                                "2026-08-17T10:00:00Z",
                                "investigator-17")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Incident not found"))
                .andExpect(jsonPath("$.incidentId").value(missingIncidentId));
    }

    @Test
    void rejectsFutureEvidenceTime() throws Exception {
        String incidentId = createIncident();
        String futureTime = Instant.now().plusSeconds(3600).toString();

        mockMvc.perform(post("/api/incidents/{incidentId}/evidence", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evidenceRequest(
                                "MEASUREMENT",
                                "Pressure measured at 0 bar.",
                                "sensor:pressure-gauge-07",
                                futureTime,
                                "investigator-17")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid evidence time"))
                .andExpect(jsonPath("$.eventAt").value(futureTime));
    }

    @Test
    void validatesEvidenceRequest() throws Exception {
        String incidentId = createIncident();

        mockMvc.perform(post("/api/incidents/{incidentId}/evidence", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"summary":" ","sourceReference":" ","submittedBy":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.type").isNotEmpty())
                .andExpect(jsonPath("$.errors.summary").isNotEmpty())
                .andExpect(jsonPath("$.errors.sourceReference").isNotEmpty())
                .andExpect(jsonPath("$.errors.eventAt").isNotEmpty())
                .andExpect(jsonPath("$.errors.submittedBy").isNotEmpty());
    }

    @Test
    void rejectsCallerAssignedProvenance() throws Exception {
        String incidentId = createIncident();

        mockMvc.perform(post("/api/incidents/{incidentId}/evidence", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type":"DOCUMENT",
                                  "summary":"Generated hypothesis",
                                  "sourceReference":"model-output:unconfirmed-01",
                                  "eventAt":"2026-08-17T10:00:00Z",
                                  "submittedBy":"investigator-17",
                                  "provenance":"MODEL_GENERATED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Request validation failed"))
                .andExpect(
                        jsonPath("$.errors.provenance").value("must be omitted; provenance is assigned by the server"));
    }

    private String createIncident() throws Exception {
        String assetBody = mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Synthetic Compressor E","type":"MACHINE"}
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
                                  "title":"Unexpected pressure loss",
                                  "severity":"HIGH",
                                  "occurredAt":"2026-08-17T08:00:00Z"
                                }
                                """.formatted(assetId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(incidentBody, "$.id");
    }

    private String appendEvidence(
            String incidentId, String type, String summary, String sourceReference, String eventAt, String submittedBy)
            throws Exception {
        return mockMvc.perform(post("/api/incidents/{incidentId}/evidence", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evidenceRequest(type, summary, sourceReference, eventAt, submittedBy)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String evidenceRequest(
            String type, String summary, String sourceReference, String eventAt, String submittedBy) {
        return """
                {
                  "type":"%s",
                  "summary":"%s",
                  "sourceReference":"%s",
                  "eventAt":"%s",
                  "submittedBy":"%s"
                }
                """.formatted(type, summary, sourceReference, eventAt, submittedBy);
    }
}
