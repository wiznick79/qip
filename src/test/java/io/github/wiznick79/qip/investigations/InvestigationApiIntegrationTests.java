package io.github.wiznick79.qip.investigations;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(properties = "qip.documents.storage-directory=target/test-investigation-storage")
class InvestigationApiIntegrationTests {

    private static final DockerImageName PGVECTOR_IMAGE =
            DockerImageName.parse("pgvector/pgvector:0.8.6-pg17-bookworm").asCompatibleSubstituteFor("postgres");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer(PGVECTOR_IMAGE);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void clearData() {
        jdbc.sql("DELETE FROM finding_review_events").update();
        jdbc.sql("DELETE FROM investigation_findings").update();
        jdbc.sql("DELETE FROM answer_citations").update();
        jdbc.sql("DELETE FROM investigation_questions").update();
        jdbc.sql("DELETE FROM investigations").update();
        jdbc.sql("DELETE FROM knowledge_passages").update();
        jdbc.sql("DELETE FROM extracted_document_pages").update();
        jdbc.sql("DELETE FROM source_documents").update();
        jdbc.sql("DELETE FROM incident_evidence").update();
        jdbc.sql("DELETE FROM incident_observations").update();
        jdbc.sql("DELETE FROM incidents").update();
        jdbc.sql("DELETE FROM assets").update();
    }

    @Test
    void createsAnInvestigationAndPersistsAGroundedQuestionWithCitations() throws Exception {
        String assetId = createAsset();
        String incidentId = createIncident(assetId);
        String documentId = uploadDocument();

        String investigationResponse = mockMvc.perform(post("/api/incidents/{incidentId}/investigations", incidentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentId").value(incidentId))
                .andExpect(jsonPath("$.questions").isEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String investigationId = JsonPath.read(investigationResponse, "$.id");

        String questionResponse = mockMvc.perform(
                        post("/api/investigations/{investigationId}/questions", investigationId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "question": "What component should be inspected for the oil leak?",
                                  "documentIds": ["%s"]
                                }
                                """.formatted(documentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GROUNDED"))
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("hydraulic seal")))
                .andExpect(jsonPath("$.citations.length()").value(1))
                .andExpect(jsonPath("$.citations[0].documentId").value(documentId))
                .andExpect(jsonPath("$.citations[0].pageNumber").value(1))
                .andExpect(jsonPath("$.modelId").value("deterministic-grounded-v1"))
                .andExpect(jsonPath("$.promptVersion").value("grounded-answer-v2"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String questionId = JsonPath.read(questionResponse, "$.id");

        String findingResponse = mockMvc.perform(post("/api/investigations/{investigationId}/findings", investigationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceQuestionId": "%s",
                                  "summary": "The hydraulic seal should be inspected before restart.",
                                  "proposedBy": "wiznick79"
                                }
                                """.formatted(questionId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.sourceQuestionId").value(questionId))
                .andExpect(jsonPath("$.reviewedBy").doesNotExist())
                .andExpect(jsonPath("$.events.length()").value(1))
                .andExpect(jsonPath("$.events[0].type").value("PROPOSED"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String findingId = JsonPath.read(findingResponse, "$.id");

        mockMvc.perform(post(
                                "/api/investigations/{investigationId}/findings/{findingId}/reviews",
                                investigationId,
                                findingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "CONFIRMED",
                                  "reviewerReference": "wiznick79",
                                  "rationale": "The cited manual and recorded observation support this inspection."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.reviewedBy").value("wiznick79"))
                .andExpect(jsonPath("$.events.length()").value(2))
                .andExpect(jsonPath("$.events[1].type").value("CONFIRMED"));

        mockMvc.perform(post(
                                "/api/investigations/{investigationId}/findings/{findingId}/reviews",
                                investigationId,
                                findingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "REJECTED",
                                  "reviewerReference": "another-reviewer",
                                  "rationale": "Attempt to overwrite the decision."
                                }
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/investigations/{investigationId}", investigationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(1))
                .andExpect(jsonPath("$.questions[0].status").value("GROUNDED"))
                .andExpect(jsonPath("$.findings.length()").value(1))
                .andExpect(jsonPath("$.findings[0].status").value("CONFIRMED"));

        Integer citationCount = jdbc.sql("SELECT count(*) FROM answer_citations")
                .query(Integer.class)
                .single();
        org.assertj.core.api.Assertions.assertThat(citationCount).isOne();
        Integer reviewEventCount = jdbc.sql("SELECT count(*) FROM finding_review_events")
                .query(Integer.class)
                .single();
        org.assertj.core.api.Assertions.assertThat(reviewEventCount).isEqualTo(2);
    }

    @Test
    void returnsInsufficientEvidenceWhenTheSelectedDocumentScopeHasNoPassages() throws Exception {
        String incidentId = createIncident(createAsset());
        String investigationResponse = mockMvc.perform(post("/api/incidents/{incidentId}/investigations", incidentId))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String investigationId = JsonPath.read(investigationResponse, "$.id");

        String response = mockMvc.perform(post("/api/investigations/{investigationId}/questions", investigationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "What evidence supports a bearing failure?",
                                  "documentIds": ["00000000-0000-0000-0000-000000000999"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INSUFFICIENT_EVIDENCE"))
                .andExpect(jsonPath("$.citations").isEmpty())
                .andExpect(
                        jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("not provide enough evidence")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String questionId = JsonPath.read(response, "$.id");
        mockMvc.perform(post("/api/investigations/{investigationId}/findings", investigationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceQuestionId": "%s",
                                  "summary": "Unsupported conclusion",
                                  "proposedBy": "wiznick79"
                                }
                                """.formatted(questionId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Only a grounded answer with citations can become a finding"));
    }

    @Test
    void creatingAnInvestigationForTheSameIncidentIsIdempotent() throws Exception {
        String incidentId = createIncident(createAsset());

        String first = mockMvc.perform(post("/api/incidents/{incidentId}/investigations", incidentId))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String second = mockMvc.perform(post("/api/incidents/{incidentId}/investigations", incidentId))
                .andReturn()
                .getResponse()
                .getContentAsString();

        org.assertj.core.api.Assertions.assertThat(JsonPath.<String>read(second, "$.id"))
                .isEqualTo(JsonPath.<String>read(first, "$.id"));
    }

    private String createAsset() throws Exception {
        String response = mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Synthetic Investigation Pump",
                                  "type": "MACHINE",
                                  "externalReference": "SYN-INV-01"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String createIncident(String assetId) throws Exception {
        String response = mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assetId": "%s",
                                  "title": "Synthetic hydraulic oil leak",
                                  "description": "Oil was observed near the pump seal.",
                                  "severity": "HIGH",
                                  "occurredAt": "2026-08-20T09:00:00Z"
                                }
                                """.formatted(assetId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String uploadDocument() throws Exception {
        String response = mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile(
                                "title", "", "text/plain", "Synthetic pump manual".getBytes(StandardCharsets.UTF_8)))
                        .file(new MockMultipartFile(
                                "file",
                                "pump.txt",
                                "text/plain",
                                "Inspect the synthetic hydraulic seal for visible oil leakage before restart."
                                        .getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("INDEXED"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.id");
    }
}
