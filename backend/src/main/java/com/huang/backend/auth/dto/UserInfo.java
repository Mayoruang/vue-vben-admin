package com.huang.backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {
    private Integer userId;
    private String username;
    private String realName;
    private String homePath;
    private List<String> roles;
    private List<String> permissions;
} 