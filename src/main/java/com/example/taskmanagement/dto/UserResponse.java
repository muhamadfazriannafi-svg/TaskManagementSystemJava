package com.example.taskmanagement.dto;

import com.example.taskmanagement.security.UserPrincipal;
import java.util.Collection;
import java.util.List;

public record UserResponse(Long id, String username, Collection<String> roles) {

    public static UserResponse from(UserPrincipal principal) {
        return new UserResponse(principal.id(), principal.username(), List.copyOf(
                principal.authorities().stream().map(a -> a.getAuthority()).toList()));
    }
}
