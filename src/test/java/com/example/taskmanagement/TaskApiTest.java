package com.example.taskmanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

/** End-to-end RBAC + filtering behaviour of the task API (FR-2.1 … FR-2.4). */
@SpringBootTest
@AutoConfigureMockMvc
class TaskApiTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    // --- helpers -----------------------------------------------------------

    private String registerAndLogin(String username, String role) throws Exception {
        String admin = login("admin", "admin12345");
        mvc.perform(post("/api/v1/auth/register").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", username,
                                "email", username + "@example.com", "password", "secret123",
                                "role", role))))
                .andExpect(status().isCreated());
        return login(username, "secret123");
    }

    private String login(String username, String password) throws Exception {
        var res = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(res.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private long userId(String token) throws Exception {
        var res = mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    }

    private ResultActions createTask(String token, String title, long assigneeId) throws Exception {
        return mvc.perform(post("/api/v1/tasks").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("title", title,
                        "description", "desc", "assignedToId", assigneeId))));
    }

    // --- tests -------------------------------------------------------------

    @Test
    void managerCreatesTaskAndAllRolesListIt() throws Exception {
        String manager = registerAndLogin("tmgr", "ROLE_MANAGER");
        String staff = registerAndLogin("tstaff", "ROLE_STAFF");
        long staffId = userId(staff);

        createTask(manager, "write report", staffId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.assignedToId").value(staffId));

        mvc.perform(get("/api/v1/tasks").header("Authorization", "Bearer " + manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title=='write report')]").exists());
        mvc.perform(get("/api/v1/tasks").header("Authorization", "Bearer " + staff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title=='write report')]").exists());
    }

    @Test
    void staffListIsFilteredToOwnAssignments() throws Exception {
        String manager = registerAndLogin("mgr2", "ROLE_MANAGER");
        String staffA = registerAndLogin("staffa", "ROLE_STAFF");
        String staffB = registerAndLogin("staffb", "ROLE_STAFF");

        createTask(manager, "task for a", userId(staffA)).andExpect(status().isCreated());
        createTask(manager, "task for b", userId(staffB)).andExpect(status().isCreated());

        mvc.perform(get("/api/v1/tasks").header("Authorization", "Bearer " + staffA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title=='task for a')]").exists())
                .andExpect(jsonPath("$[?(@.title=='task for b')]").doesNotExist());

        mvc.perform(get("/api/v1/tasks").header("Authorization", "Bearer " + manager))
                .andExpect(jsonPath("$[?(@.title=='task for a')]").exists())
                .andExpect(jsonPath("$[?(@.title=='task for b')]").exists());
    }

    @Test
    void staffCannotCreateUpdateOrDeleteButCanMoveOwnStatus() throws Exception {
        String manager = registerAndLogin("mgr3", "ROLE_MANAGER");
        String staff = registerAndLogin("staffc", "ROLE_STAFF");
        String other = registerAndLogin("staffd", "ROLE_STAFF");
        long staffId = userId(staff);

        var created = createTask(manager, "ship it", staffId).andExpect(status().isCreated()).andReturn();
        long taskId = json.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        createTask(staff, "nope", staffId).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/tasks/" + taskId).header("Authorization", "Bearer " + staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "hijack"))))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/tasks/" + taskId).header("Authorization", "Bearer " + staff))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/v1/tasks/" + taskId + "/status").header("Authorization", "Bearer " + staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // staff may not touch a task assigned to someone else
        mvc.perform(patch("/api/v1/tasks/" + taskId + "/status")
                        .header("Authorization", "Bearer " + other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/v1/tasks/" + taskId + "/status")
                        .header("Authorization", "Bearer " + manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void managerUpdatesDetailsAndOnlyAdminDeletes() throws Exception {
        String manager = registerAndLogin("mgr4", "ROLE_MANAGER");
        String staff = registerAndLogin("staffe", "ROLE_STAFF");
        var created = createTask(manager, "draft", userId(staff)).andReturn();
        long taskId = json.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(put("/api/v1/tasks/" + taskId).header("Authorization", "Bearer " + manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "final", "description", "v2"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("final"))
                .andExpect(jsonPath("$.description").value("v2"));

        mvc.perform(delete("/api/v1/tasks/" + taskId).header("Authorization", "Bearer " + manager))
                .andExpect(status().isForbidden());

        mvc.perform(delete("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + login("admin", "admin12345")))
                .andExpect(status().isNoContent());
    }

    @Test
    void unknownTaskOrAssigneeIs404AndBadPayloadIs400() throws Exception {
        String manager = registerAndLogin("mgr5", "ROLE_MANAGER");

        createTask(manager, "x", 999_999).andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/tasks/999999").header("Authorization", "Bearer " + manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "t"))))
                .andExpect(status().isNotFound());

        mvc.perform(patch("/api/v1/tasks/1/status").header("Authorization", "Bearer " + manager)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"NOPE\"}"))
                .andExpect(status().isBadRequest());

        assertThat(manager).isNotBlank();
    }
}
