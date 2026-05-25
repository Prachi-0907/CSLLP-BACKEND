package com.configserverllp.csllp_learning_platform.material_service.service.impl;

import com.configserverllp.csllp_learning_platform.material_service.dto.MaterialRequest;
import com.configserverllp.csllp_learning_platform.material_service.entity.Material;
import com.configserverllp.csllp_learning_platform.material_service.exception.BadRequestException;
import com.configserverllp.csllp_learning_platform.material_service.exception.ResourceNotFoundException;
import com.configserverllp.csllp_learning_platform.material_service.repository.MaterialRepository;
import com.configserverllp.csllp_learning_platform.material_service.service.MaterialService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
@Transactional
public class MaterialServiceImpl implements MaterialService {

    private final MaterialRepository materialRepository;
    private final RestTemplate restTemplate;

    private final Map<String, Path> storagePaths = new HashMap<>();

    @Value("${user.service.base-url}")
    private String userServiceBaseUrl;

    public MaterialServiceImpl(MaterialRepository materialRepository,
                               RestTemplate restTemplate,
                               @Value("${material.upload.videos-dir}") String videosDir,
                               @Value("${material.upload.docs-dir}") String docsDir,
                               @Value("${material.upload.pdfs-dir}") String pdfsDir,
                               @Value("${material.upload.links-dir}") String linksDir,
                               @Value("${material.upload.base-dir}") String baseDir) {
        this.materialRepository = materialRepository;
        this.restTemplate = restTemplate;

        storagePaths.put("video", Paths.get(videosDir));
        storagePaths.put("document", Paths.get(docsDir));
        storagePaths.put("pdf", Paths.get(pdfsDir));
        storagePaths.put("link", Paths.get(linksDir));

        // ensure all directories exist
        for (Path path : storagePaths.values()) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException("Could not create directory: " + path, e);
            }
        }

        // also create base dir if needed
        try {
            Files.createDirectories(Paths.get(baseDir));
        } catch (IOException e) {
            throw new RuntimeException("Could not create base directory: " + baseDir, e);
        }
    }


    @Override
    public Material createMaterial(MaterialRequest request) {
        if (request.getTitle() == null || request.getTitle().isEmpty()) {
            throw new BadRequestException("Title is required");
        }

        // Call User Service to validate uploader
        @SuppressWarnings("unchecked")
        Map<String,Object> apiResp = restTemplate.getForObject(userServiceBaseUrl + "/api/users/" + request.getUploadedBy(), Map.class);

        if (apiResp == null || apiResp.get("data") == null) {
            throw new BadRequestException("Uploader not found in User Service");
        }

        @SuppressWarnings("unchecked")
        Map<String,Object> userMap = (Map<String,Object>) apiResp.get("data");
        String role = userMap.get("role") == null ? null : userMap.get("role").toString();

        if (role == null) throw new BadRequestException("User role not found");
        role = role.toUpperCase();
        if (!(role.contains("ADMIN") || role.contains("MANAGER") || role.contains("HR"))) {
            throw new BadRequestException("Only Admin, Manager or HR can upload materials");
        }

        Material material = new Material();
        material.setTitle(request.getTitle());
        material.setDescription(request.getDescription());
        material.setFileUrl(request.getFileUrl());
        material.setTags(request.getTags());
        material.setUploadedBy(request.getUploadedBy());
        material.setType(request.getType());
        material.setCourseId(request.getCourseId());
        material.setStatus(Material.Status.ACTIVE);

        return materialRepository.save(material);
    }


    @Override
    public Material updateMaterial(Long id, MaterialRequest request, MultipartFile file) {
        Material existing = getMaterialById(id);

        // Update metadata
        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setTags(request.getTags());
        existing.setType(request.getType() != null ? request.getType() : existing.getType());
        existing.setCourseId(request.getCourseId() != null ? request.getCourseId() : existing.getCourseId());

        // Replace file if a new file is provided
        if (file != null && !file.isEmpty()) {
            String type = request.getType() != null ? request.getType() : existing.getType();
            String relativePath = storeFile(file, type);
            existing.setFileUrl(relativePath);
        }

        return materialRepository.save(existing);
    }



    @Override
    public void deleteMaterial(Long id) {
        Material material = getMaterialById(id);
        material.setStatus(Material.Status.INACTIVE);
        materialRepository.save(material);
    }

    @Override
    public Material getMaterialById(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with id " + id));
    }

    @Override
    public List<Material> getAllMaterials() {
        return materialRepository.findByStatus(Material.Status.ACTIVE);
    }


    @Override
    public List<Material> getMaterialsByUploader(Long uploaderId) {
        return materialRepository.findByUploadedBy(uploaderId);
    }

    @Override
    public List<Material> searchMaterialsByTag(String keyword) {
        return materialRepository.findByTagsContainingIgnoreCase(keyword);
    }


    @Override
    public List<Material> getMaterialsByCourse(Long courseId) {
        return materialRepository.findByCourseId(courseId);
    }



    @Override
    public String storeFile(MultipartFile file, String type) {
        Path basePath = null;

        if (type != null) {
            String key = type.toLowerCase();
            if (storagePaths.containsKey(key)) {
                basePath = storagePaths.get(key);
            }
        }
        if (basePath == null) {
            basePath = storagePaths.get("document"); // default
        }

        try {
            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path targetPath = basePath.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return basePath.getFileName().toString() + "/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }

    @Override
    public Resource loadFileAsResource(String relativePath) {
        Path filePath = Paths.get(System.getProperty("user.dir"))
                .resolve(relativePath)
                .normalize();

        if (!Files.exists(filePath)) {
            throw new RuntimeException("File not found: " + relativePath);
        }

        return new FileSystemResource(filePath);
    }


    @Data
    static class UserResponse {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
        private String role;
        private Long managerId;
        private String status;
    }
}
