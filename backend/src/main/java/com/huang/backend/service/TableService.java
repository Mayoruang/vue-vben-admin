package com.huang.backend.service;

import com.huang.backend.payload.response.PageResponse;

import java.util.Map;

public interface TableService {
    PageResponse<Map<String, Object>> getTableData(int page, int pageSize, String sortBy, String sortOrder);
} 