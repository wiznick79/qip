package io.github.wiznick79.qip;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(properties = "qip.security.enabled=true")
class SecurityIntegrationTests {

    private static final DockerImageName PGVECTOR_IMAGE =
            DockerImageName.parse("pgvector/pgvector:0.8.6-pg17-bookworm").asCompatibleSubstituteFor("postgres");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer(PGVECTOR_IMAGE);

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsSessionAndCsrfInformationBeforeAuthentication() throws Exception {
        mockMvc.perform(get("/api/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.username").doesNotExist())
                .andExpect(jsonPath("$.roles").isEmpty())
                .andExpect(jsonPath("$.csrfHeaderName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.csrfToken").isNotEmpty());
    }

    @Test
    void rejectsAnonymousApiAccessWithSafeProblemDetails() throws Exception {
        mockMvc.perform(get("/api/assets"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Authentication required"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void restrictsOperationalMetricsToAdministrators() throws Exception {
        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/metrics").with(user("investigator").roles("INVESTIGATOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/metrics").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());
    }

    @Test
    void acceptsConfiguredLocalCredentials() throws Exception {
        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/session/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "qip-investigator")
                        .param("password", "qip-investigator-local-only"))
                .andExpect(status().isNoContent())
                .andReturn()
                .getRequest()
                .getSession(false);

        mockMvc.perform(get("/api/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value("qip-investigator"))
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.roles[0]").value("INVESTIGATOR"));
    }

    @Test
    void rejectsInvalidCredentialsWithSafeProblemDetails() throws Exception {
        mockMvc.perform(post("/api/session/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "qip-investigator")
                        .param("password", "incorrect"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Authentication failed"));
    }

    @Test
    void enforcesIndependentReviewerAndInvestigatorRoles() throws Exception {
        String reviewPath = "/api/investigations/00000000-0000-0000-0000-000000000001"
                + "/findings/00000000-0000-0000-0000-000000000002/reviews";
        mockMvc.perform(post(reviewPath)
                        .with(user("investigator").roles("INVESTIGATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "CONFIRMED",
                                  "rationale": "Synthetic review rationale."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Access denied"));

        mockMvc.perform(post("/api/investigations/00000000-0000-0000-0000-000000000001/closure")
                        .with(user("reviewer").roles("REVIEWER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "summary": "Synthetic closure summary."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Access denied"));
    }

    @Test
    void derivesStoredActorFromAuthenticatedPrincipal() throws Exception {
        String assetBody = mockMvc.perform(post("/api/assets")
                        .with(user("wiznick79").roles("INVESTIGATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Authenticated Synthetic Pump","type":"MACHINE"}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String assetId = JsonPath.read(assetBody, "$.id");

        String incidentBody = mockMvc.perform(post("/api/incidents")
                        .with(user("wiznick79").roles("INVESTIGATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assetId":"%s",
                                  "title":"Authenticated attribution check",
                                  "severity":"LOW",
                                  "occurredAt":"2026-08-20T08:00:00Z"
                                }
                                """.formatted(assetId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String incidentId = JsonPath.read(incidentBody, "$.id");

        mockMvc.perform(post("/api/incidents/{incidentId}/observations", incidentId)
                        .with(user("wiznick79").roles("INVESTIGATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text":"Authenticated observation.",
                                  "authorReference":"spoofed-user",
                                  "observedAt":"2026-08-20T08:01:00Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorReference").value("wiznick79"));
    }
}
