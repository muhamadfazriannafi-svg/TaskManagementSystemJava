package com.example.taskmanagement.service;

import com.example.taskmanagement.dto.UserSummary;
import com.example.taskmanagement.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /** Backs the assignee picker; ADMIN/MANAGER only (enforced in SecurityConfig). */
    @Transactional(readOnly = true)
    public List<UserSummary> list() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(u -> u.getUsername().toLowerCase()))
                .map(UserSummary::from)
                .toList();
    }
}
