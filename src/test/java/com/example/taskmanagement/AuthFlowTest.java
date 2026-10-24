package com.example.taskmanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    private String token() throws Exception {
        MvcResult res = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "username", "admin", "password", "admin12345"))))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(res.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    void loginIssuesTokenAndMeEchoesPrincipal() throws Exception {
        String jwt = token();
        assertThat(jwt).isNotBlank();

        MvcResult me = mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn();
        var body = json.readTree(me.getResponse().getContentAsString());
        assertThat(body.get("username").asText()).isEqualTo("admin");
        assertThat(body.get("roles").toString()).contains("ROLE_ADMIN");
    }

    @Test
    void tasksEndpointRejectsMissingOrBogusToken() throws Exception {
        mvc.perform(get("/api/v1/tasks")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/tasks").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerRequiresAdmin() throws Exception {
        String body = json.writeValueAsString(Map.of("username", "bob", "email", "bob@example.com",
                "password", "secret123", "role", "ROLE_STAFF"));

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/auth/register").header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void staffTokenCannotReachAdminOnlySurface() throws Exception {
        String admin = token();
        mvc.perform(post("/api/v1/auth/register").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", "carol",
                                "email", "carol@example.com", "password", "secret123",
                                "role", "ROLE_STAFF"))))
                .andExpect(status().isCreated());

        MvcResult res = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", "carol", "password", "secret123"))))
                .andExpect(status().isOk()).andReturn();
        String staff = json.readTree(res.getResponse().getContentAsString()).get("accessToken").asText();

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + staff))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/register").header("Authorization", "Bearer " + staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", "dave",
                                "email", "dave@example.com", "password", "secret123",
                                "role", "ROLE_STAFF"))))
                .andExpect(status().isForbidden());
    }
}
