package com.example.taskmanagement;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

@SpringBootTest
@AutoConfigureMockMvc
class UserListTest {

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

    @Test
    void managersCanListUsersButStaffCannot() throws Exception {
        String admin = login("admin", "admin12345");
        mvc.perform(post("/api/v1/auth/register").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", "ulstaff",
                                "email", "ulstaff@example.com", "password", "secret123",
                                "role", "ROLE_STAFF"))))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username=='admin')]").exists())
                .andExpect(jsonPath("$[0].roles").isArray());

        mvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + login("ulstaff", "secret123")))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
    }
}
