package com.huang.backend.service;

import com.huang.backend.payload.response.UserInfoResponse;

public interface UserService {
    UserInfoResponse getUserInfo(String username);
} 