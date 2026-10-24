package com.example.taskmanagement;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.taskmanagement.entity.Role;
import com.example.taskmanagement.entity.Task;
import com.example.taskmanagement.entity.TaskStatus;
import com.example.taskmanagement.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class EntityMappingTest {

    @Autowired
    EntityManager em;

    @Test
    void persistingUserTaskAndRoleHitsExpectedTables() {
        var role = new Role("ROLE_STAFF");
        var user = new User("alice", "alice@example.com", "x");
        user.getRoles().add(role);
        var task = new Task("Fix bug", "desc", user, user);
        em.persist(role);
        em.persist(user);
        em.persist(task);
        em.flush();
        em.clear();

        assertThat(em.find(User.class, user.getId()).getRoles()).hasSize(1);
        assertThat(em.find(Task.class, task.getId()).getStatus()).isEqualTo(TaskStatus.TODO);

        var tables = em.createNativeQuery(
                        "select lower(table_name) from information_schema.tables where lower(table_schema) = 'public'")
                .getResultList();
        assertThat(tables).contains("user_m", "role_m", "user_role_m", "task_t");
    }
}
