package io.github.wiznick79.qip.knowledge;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.wiznick79.qip.knowledge.api.KnowledgeQuery;
import io.github.wiznick79.qip.knowledge.api.KnowledgeSearch;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
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
@SpringBootTest(properties = "qip.security.enabled=false")
@TestPropertySource(properties = "qip.documents.storage-directory=target/test-document-storage")
class DocumentApiIntegrationTests {

    private static final DockerImageName PGVECTOR_IMAGE =
            DockerImageName.parse("pgvector/pgvector:0.8.6-pg17-bookworm").asCompatibleSubstituteFor("postgres");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer(PGVECTOR_IMAGE);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private KnowledgeSearch knowledgeSearch;

    @BeforeEach
    void clearDocuments() {
        jdbcClient.sql("DELETE FROM extracted_document_pages").update();
        jdbcClient.sql("DELETE FROM source_documents").update();
    }

    @Test
    void uploadsExtractsAndReturnsPlainTextMetadataAndStatus() throws Exception {
        String response = upload("../synthetic-guide.txt", "text/plain", "Inspect the synthetic bearing.", true);
        String documentId = JsonPath.read(response, "$.id");

        mockMvc.perform(get("/api/documents/{documentId}", documentId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(documentId))
                .andExpect(jsonPath("$.title").value("Synthetic bearing guide"))
                .andExpect(jsonPath("$.originalFilename").value("synthetic-guide.txt"))
                .andExpect(jsonPath("$.mediaType").value("text/plain"))
                .andExpect(jsonPath("$.status").value("INDEXED"))
                .andExpect(jsonPath("$.failureReason").doesNotExist())
                .andExpect(jsonPath("$.extractedPageCount").value(1));

        mockMvc.perform(get("/api/documents/{documentId}/status", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INDEXED"))
                .andExpect(jsonPath("$.extractedPageCount").value(1));

        String extracted = jdbcClient
                .sql("SELECT text FROM extracted_document_pages WHERE document_id = :id")
                .param("id", java.util.UUID.fromString(documentId))
                .query(String.class)
                .single();
        org.assertj.core.api.Assertions.assertThat(extracted).isEqualTo("Inspect the synthetic bearing.");
    }

    @Test
    void makesRepeatedContentUploadsIdempotent() throws Exception {
        String first = upload("first.txt", "text/plain", "Identical synthetic content", true);
        String firstId = JsonPath.read(first, "$.id");

        var duplicate = mockMvc.perform(multipart("/api/documents")
                        .file(title("A different title"))
                        .file(file("second.txt", "text/plain", "Identical synthetic content")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstId))
                .andReturn()
                .getResponse()
                .getContentAsString();

        org.assertj.core.api.Assertions.assertThat(JsonPath.<String>read(duplicate, "$.title"))
                .isEqualTo("Synthetic bearing guide");
        Integer count = jdbcClient
                .sql("SELECT count(*) FROM source_documents")
                .query(Integer.class)
                .single();
        org.assertj.core.api.Assertions.assertThat(count).isOne();
    }

    @Test
    void duplicateUploadResumesAnExtractedDocumentFromAnEarlierApplicationVersion() throws Exception {
        String first = upload("legacy.txt", "text/plain", "Legacy extracted synthetic content", true);
        UUID documentId = UUID.fromString(JsonPath.read(first, "$.id"));
        jdbcClient
                .sql("DELETE FROM knowledge_passages WHERE document_id = :id")
                .param("id", documentId)
                .update();
        jdbcClient
                .sql("UPDATE source_documents SET ingestion_status = 'EXTRACTED' WHERE id = :id")
                .param("id", documentId)
                .update();

        mockMvc.perform(multipart("/api/documents")
                        .file(title("Legacy duplicate"))
                        .file(file("duplicate.txt", "text/plain", "Legacy extracted synthetic content")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(documentId.toString()))
                .andExpect(jsonPath("$.status").value("INDEXED"));

        Integer count = jdbcClient
                .sql("SELECT count(*) FROM knowledge_passages WHERE document_id = :id")
                .param("id", documentId)
                .query(Integer.class)
                .single();
        org.assertj.core.api.Assertions.assertThat(count).isOne();
    }

    @Test
    void indexesExtractedPassagesAndSearchesWithDocumentFilters() throws Exception {
        String pumpResponse =
                upload("pump.txt", "text/plain", "Hydraulic pump seal inspection for a synthetic oil leak", true);
        String conveyorResponse =
                upload("conveyor.txt", "text/plain", "Packaging conveyor belt alignment procedure", true);
        UUID pumpId = UUID.fromString(JsonPath.read(pumpResponse, "$.id"));
        UUID conveyorId = UUID.fromString(JsonPath.read(conveyorResponse, "$.id"));

        var allResults = knowledgeSearch.search(new KnowledgeQuery("hydraulic pump leak", Set.of(), 5));
        var filteredResults = knowledgeSearch.search(new KnowledgeQuery("hydraulic pump leak", Set.of(conveyorId), 5));

        org.assertj.core.api.Assertions.assertThat(allResults).isNotEmpty();
        org.assertj.core.api.Assertions.assertThat(allResults.getFirst().documentId())
                .isEqualTo(pumpId);
        org.assertj.core.api.Assertions.assertThat(filteredResults)
                .extracting(io.github.wiznick79.qip.knowledge.api.RetrievedPassage::documentId)
                .containsOnly(conveyorId);
        Integer passageCount = jdbcClient
                .sql("SELECT count(*) FROM knowledge_passages")
                .query(Integer.class)
                .single();
        org.assertj.core.api.Assertions.assertThat(passageCount).isEqualTo(2);
    }

    @Test
    void listsDocumentsWithBoundedMetadata() throws Exception {
        upload("first.txt", "text/plain", "First synthetic document", true);
        upload("second.txt", "text/plain", "Second synthetic document", true);

        mockMvc.perform(get("/api/documents").queryParam("page", "0").queryParam("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].status").value("INDEXED"))
                .andExpect(jsonPath("$.items[0].extractedPageCount").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void rejectsMediaTypeSpoofingBeforeCreatingMetadata() throws Exception {
        mockMvc.perform(multipart("/api/documents")
                        .file(title("Spoofed PDF"))
                        .file(file("guide.pdf", "application/pdf", "not really a PDF")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid document upload"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("does not match")));

        Integer count = jdbcClient
                .sql("SELECT count(*) FROM source_documents")
                .query(Integer.class)
                .single();
        org.assertj.core.api.Assertions.assertThat(count).isZero();
    }

    @Test
    void recordsDeterministicExtractionFailureAndSupportsRetry() throws Exception {
        String response = upload("broken.pdf", "application/pdf", "%PDF-broken", true);
        String documentId = JsonPath.read(response, "$.id");

        mockMvc.perform(get("/api/documents/{documentId}/status", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXTRACTION_FAILED"))
                .andExpect(jsonPath("$.failureReason").value("PDF content could not be parsed"));

        mockMvc.perform(post("/api/documents/{documentId}/extraction", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXTRACTION_FAILED"))
                .andExpect(jsonPath("$.failureReason").value("PDF content could not be parsed"));
    }

    @Test
    void rejectsInvalidUtf8AndUnsupportedMediaTypes() throws Exception {
        mockMvc.perform(multipart("/api/documents")
                        .file(title("Invalid text"))
                        .file(new MockMultipartFile(
                                "file", "invalid.txt", "text/plain", new byte[] {(byte) 0xC3, 0x28})))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Plain-text documents must use valid UTF-8"));

        mockMvc.perform(multipart("/api/documents")
                        .file(title("Unsupported image"))
                        .file(file("photo.png", "image/png", "synthetic")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Only PDF and plain-text documents are accepted"));
    }

    @Test
    void returnsProblemDetailsForMissingDocumentAndMultipartFields() throws Exception {
        String missingId = "00000000-0000-0000-0000-000000000999";

        mockMvc.perform(get("/api/documents/{documentId}", missingId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Document not found"))
                .andExpect(jsonPath("$.documentId").value(missingId));

        mockMvc.perform(multipart("/api/documents").file(title("Missing file")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Required part 'file' is not present."));
    }

    private String upload(String filename, String mediaType, String contents, boolean created) throws Exception {
        var action = mockMvc.perform(multipart("/api/documents")
                .file(title("  Synthetic bearing guide  "))
                .file(file(filename, mediaType, contents)));
        if (created) {
            action.andExpect(status().isCreated())
                    .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/documents/.+")));
        }
        return action.andReturn().getResponse().getContentAsString();
    }

    private static MockMultipartFile title(String value) {
        return new MockMultipartFile("title", "", "text/plain", value.getBytes(StandardCharsets.UTF_8));
    }

    private static MockMultipartFile file(String filename, String mediaType, String value) {
        return new MockMultipartFile("file", filename, mediaType, value.getBytes(StandardCharsets.UTF_8));
    }
}
