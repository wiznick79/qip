package io.github.wiznick79.qip.incidents;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
class IncidentApiIntegrationTests {

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
        jdbcClient.sql("DELETE FROM incidents").update();
        jdbcClient.sql("DELETE FROM assets").update();
    }

    @Test
    void createsAndRetrievesReportedIncident() throws Exception {
        String assetId = createAsset("Synthetic Press A");
        String body = createIncident(assetId, "  Hydraulic pressure loss  ", "HIGH", "2026-08-16T10:00:00Z");
        String incidentId = JsonPath.read(body, "$.id");

        mockMvc.perform(get("/api/incidents/{incidentId}", incidentId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(incidentId))
                .andExpect(jsonPath("$.assetId").value(assetId))
                .andExpect(jsonPath("$.title").value("Hydraulic pressure loss"))
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.status").value("REPORTED"))
                .andExpect(jsonPath("$.occurredAt").value("2026-08-16T10:00:00Z"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void updatesStatusAndRejectsInvalidTransition() throws Exception {
        String assetId = createAsset("Synthetic Mixer B");
        String incidentId = JsonPath.read(
                createIncident(assetId, "Unexpected vibration", "MEDIUM", "2026-08-16T11:00:00Z"), "$.id");

        updateStatus(incidentId, "INVESTIGATING").andExpect(jsonPath("$.status").value("INVESTIGATING"));

        mockMvc.perform(patch("/api/incidents/{incidentId}/status", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CLOSED"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid incident transition"))
                .andExpect(jsonPath("$.currentStatus").value("INVESTIGATING"))
                .andExpect(jsonPath("$.requestedStatus").value("CLOSED"));
    }

    @Test
    void searchesByAssetStatusAndHalfOpenTimeRange() throws Exception {
        String firstAssetId = createAsset("Synthetic Line One");
        String secondAssetId = createAsset("Synthetic Line Two");
        String matchingIncidentId =
                JsonPath.read(createIncident(firstAssetId, "Matching incident", "LOW", "2026-08-16T12:00:00Z"), "$.id");
        createIncident(firstAssetId, "Outside range", "LOW", "2026-08-17T12:00:00Z");
        createIncident(secondAssetId, "Different asset", "LOW", "2026-08-16T13:00:00Z");
        updateStatus(matchingIncidentId, "INVESTIGATING");

        mockMvc.perform(get("/api/incidents")
                        .queryParam("assetId", firstAssetId)
                        .queryParam("status", "INVESTIGATING")
                        .queryParam("from", "2026-08-16T00:00:00Z")
                        .queryParam("to", "2026-08-17T00:00:00Z")
                        .queryParam("page", "0")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(matchingIncidentId))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listsNewestIncidentsFirstWithBoundedPagination() throws Exception {
        String assetId = createAsset("Synthetic Conveyor C");
        createIncident(assetId, "Older incident", "LOW", "2026-08-15T12:00:00Z");
        createIncident(assetId, "Newer incident", "LOW", "2026-08-16T12:00:00Z");

        mockMvc.perform(get("/api/incidents").queryParam("page", "0").queryParam("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Newer incident"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void rejectsReversedSearchRange() throws Exception {
        mockMvc.perform(get("/api/incidents")
                        .queryParam("from", "2026-08-17T00:00:00Z")
                        .queryParam("to", "2026-08-16T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid incident search range"));
    }

    @Test
    void rejectsIncidentForMissingAsset() throws Exception {
        String missingAssetId = "00000000-0000-0000-0000-000000000999";

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createIncidentRequest(
                                missingAssetId, "Coolant leak", "MEDIUM", "2026-08-16T10:00:00Z")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Incident asset not found"))
                .andExpect(jsonPath("$.assetId").value(missingAssetId));
    }

    @Test
    void validatesCreateRequest() throws Exception {
        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":" ","severity":"HIGH"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.assetId").isNotEmpty())
                .andExpect(jsonPath("$.errors.title").isNotEmpty())
                .andExpect(jsonPath("$.errors.occurredAt").isNotEmpty());
    }

    private String createAsset(String name) throws Exception {
        String request = """
                {
                  "name": "%s",
                  "type": "MACHINE"
                }
                """.formatted(name);
        String body = mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String createIncident(String assetId, String title, String severity, String occurredAt) throws Exception {
        return mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createIncidentRequest(assetId, title, severity, occurredAt)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/incidents/.+")))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private org.springframework.test.web.servlet.ResultActions updateStatus(String incidentId, String statusValue)
            throws Exception {
        return mockMvc.perform(patch("/api/incidents/{incidentId}/status", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"%s"}
                                """.formatted(statusValue)))
                .andExpect(status().isOk());
    }

    private String createIncidentRequest(String assetId, String title, String severity, String occurredAt) {
        return """
                {
                  "assetId": "%s",
                  "title": "%s",
                  "description": "Synthetic observation only.",
                  "severity": "%s",
                  "occurredAt": "%s"
                }
                """.formatted(assetId, title, severity, occurredAt);
    }
}
