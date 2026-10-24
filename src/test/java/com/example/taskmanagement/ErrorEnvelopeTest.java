package com.example.taskmanagement;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Every failure path must produce the same ApiError envelope. */
@SpringBootTest
@AutoConfigureMockMvc
class ErrorEnvelopeTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    private String login(String username, String password) throws Exception {
        var res = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(res.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private static ResultActions expectEnvelope(ResultActions actions, int status, String error) throws Exception {
        return actions
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.error").value(error))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void unauthorizedFromFilterChainUsesEnvelope() throws Exception {
        expectEnvelope(mvc.perform(get("/api/v1/tasks")), 401, "Unauthorized")
                .andExpect(jsonPath("$.path").value("/api/v1/tasks"));
        expectEnvelope(mvc.perform(get("/api/v1/tasks").header("Authorization", "Bearer junk")),
                401, "Unauthorized");
    }

    @Test
    void badCredentialsUseEnvelope() throws Exception {
        expectEnvelope(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", "admin", "password", "wrong")))), 401,
                "Unauthorized");
    }

    @Test
    void validationErrorsListEveryField() throws Exception {
        String admin = login("admin", "admin12345");
        expectEnvelope(mvc.perform(post("/api/v1/auth/register").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{}")), 400, "Bad Request")
                .andExpect(jsonPath("$.details.length()").value(4));
    }

    @Test
    void forbiddenAndNotFoundAndMalformedUseEnvelope() throws Exception {
        String admin = login("admin", "admin12345");
        String body = json.writeValueAsString(Map.of("username", "eve", "email", "eve@example.com",
                "password", "secret123", "role", "ROLE_STAFF"));
        mvc.perform(post("/api/v1/auth/register").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        String staff = login("eve", "secret123");

        expectEnvelope(mvc.perform(delete("/api/v1/tasks/1").header("Authorization", "Bearer " + staff)),
                403, "Forbidden");
        expectEnvelope(mvc.perform(put("/api/v1/tasks/999999").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "t")))),
                404, "Not Found");
        expectEnvelope(mvc.perform(put("/api/v1/tasks/abc").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"t\"}")), 400, "Bad Request");
        expectEnvelope(mvc.perform(post("/api/v1/tasks").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{not json")), 400, "Bad Request");
        expectEnvelope(mvc.perform(post("/api/v1/auth/register").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(
                        status().isBadRequest()),
                400, "Bad Request")
                .andExpect(jsonPath("$.message").value("Username already taken"));
        expectEnvelope(mvc.perform(get("/api/v1/nope").header("Authorization", "Bearer " + admin)),
                404, "Not Found");
    }
}
