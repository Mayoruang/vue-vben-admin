package com.huang.backend.controller;

import com.huang.backend.payload.response.ApiResponse;
import com.huang.backend.payload.response.PageResponse;
import com.huang.backend.service.TableService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/table")
@RequiredArgsConstructor
public class TableController {

    private final TableService tableService;

    @GetMapping("/list")
    public ApiResponse<PageResponse<Map<String, Object>>> getTableData(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder
    ) {
        return ApiResponse.success(tableService.getTableData(page, pageSize, sortBy, sortOrder));
    }
} 