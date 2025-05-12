package com.huang.backend.service.impl;

import com.huang.backend.model.User;
import com.huang.backend.payload.response.UserInfoResponse;
import com.huang.backend.repository.UserRepository;
import com.huang.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserInfoResponse getUserInfo(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        return UserInfoResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .homePath(user.getHomePath())
                .roles(user.getRoles().stream()
                        .map(role -> role.getRoleCode())
                        .collect(Collectors.toList()))
                .build();
    }
} 