package com.huang.backend.controller;

import com.huang.backend.model.UploadedFile;
import com.huang.backend.model.User;
import com.huang.backend.payload.response.ApiResponse;
import com.huang.backend.repository.UploadedFileRepository;
import com.huang.backend.repository.UserRepository;
import com.huang.backend.security.service.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class FileController {

    private final UserRepository userRepository;
    private final UploadedFileRepository uploadedFileRepository;

    @Value("${application.base-url}")
    private String baseUrl;

    private final Path fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();

    @PostMapping
    public ApiResponse<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        try {
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 创建存储目录
            if (!Files.exists(fileStorageLocation)) {
                Files.createDirectories(fileStorageLocation);
            }

            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String storageName = UUID.randomUUID() + fileExtension;
            
            // 保存文件
            Path targetLocation = fileStorageLocation.resolve(storageName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 保存文件信息到数据库
            UploadedFile uploadedFile = UploadedFile.builder()
                    .user(user)
                    .originalName(originalFilename)
                    .storageName(storageName)
                    .filePath(targetLocation.toString())
                    .fileSize(file.getSize())
                    .fileType(file.getContentType())
                    .build();
            
            uploadedFileRepository.save(uploadedFile);

            // 返回文件URL
            String fileUrl = baseUrl + "/api/upload/" + storageName;
            Map<String, String> result = new HashMap<>();
            result.put("url", fileUrl);
            return ApiResponse.success(result);
        } catch (IOException e) {
            log.error("Could not store file", e);
            return ApiResponse.error("Could not store file. Error: " + e.getMessage());
        }
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        try {
            Path filePath = fileStorageLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            log.error("File not found", e);
            return ResponseEntity.badRequest().build();
        }
    }
} 