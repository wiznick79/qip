package io.github.wiznick79.qip.assets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
@SpringBootTest
class AssetApiIntegrationTests {

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
    void clearAssets() {
        jdbcClient.sql("DELETE FROM assets").update();
    }

    @Test
    void createsAndRetrievesAnAsset() throws Exception {
        String body = createAsset("  Forming Press 04  ", "PRESS-04");
        String assetId = JsonPath.read(body, "$.id");

        mockMvc.perform(get("/api/assets/{assetId}", assetId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(assetId))
                .andExpect(jsonPath("$.name").value("Forming Press 04"))
                .andExpect(jsonPath("$.type").value("MACHINE"))
                .andExpect(jsonPath("$.externalReference").value("PRESS-04"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void listsAssetsWithBoundedDeterministicPagination() throws Exception {
        createAsset("Zulu Mixer", null);
        createAsset("Alpha Press", null);

        mockMvc.perform(get("/api/assets").queryParam("page", "0").queryParam("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Alpha Press"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void returnsProblemDetailsForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " ",
                                  "type": "MACHINE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.name").isNotEmpty());
    }

    @Test
    void returnsProblemDetailsForMissingAsset() throws Exception {
        String missingId = "00000000-0000-0000-0000-000000000999";

        mockMvc.perform(get("/api/assets/{assetId}", missingId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Asset not found"))
                .andExpect(jsonPath("$.assetId").value(missingId));
    }

    private String createAsset(String name, String externalReference) throws Exception {
        String externalReferenceJson = externalReference == null ? "null" : "\"" + externalReference + "\"";
        String request = """
                {
                  "name": "%s",
                  "type": "MACHINE",
                  "externalReference": %s
                }
                """.formatted(name, externalReferenceJson);

        return mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/assets/.+")))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}
