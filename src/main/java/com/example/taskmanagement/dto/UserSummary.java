package com.example.taskmanagement.dto;

import com.example.taskmanagement.entity.User;
import java.util.List;

public record UserSummary(Long id, String username, List<String> roles) {

    public static UserSummary from(User user) {
        return new UserSummary(user.getId(), user.getUsername(),
                user.getRoles().stream().map(r -> r.getName()).sorted().toList());
    }
}
