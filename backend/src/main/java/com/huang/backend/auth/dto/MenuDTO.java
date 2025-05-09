package com.huang.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MenuDTO {
    private Integer id;
    private String path;
    private String name;
    private String component;
    private String redirect;
    private Map<String, Object> meta;
    private List<MenuDTO> children;
} 