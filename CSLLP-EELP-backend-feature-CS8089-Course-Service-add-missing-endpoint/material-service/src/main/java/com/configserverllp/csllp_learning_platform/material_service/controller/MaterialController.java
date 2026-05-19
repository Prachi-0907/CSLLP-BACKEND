package com.configserverllp.csllp_learning_platform.material_service.controller;

import com.configserverllp.csllp_learning_platform.material_service.dto.MaterialRequest;
import com.configserverllp.csllp_learning_platform.material_service.dto.MaterialResponse;
import com.configserverllp.csllp_learning_platform.material_service.entity.Material;
import com.configserverllp.csllp_learning_platform.material_service.service.MaterialService;
import com.configserverllp.csllp_learning_platform.material_service.storage.FileStorageService;
import com.configserverllp.csllp_learning_platform.material_service.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.http.MediaTypeFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;
    private final FileStorageService fileStorageService;

    // existing create (JSON-based metadata create) - unchanged
    @PostMapping
    public ResponseEntity<ApiResponse<Material>> create(@Valid @RequestBody MaterialRequest request) {
        Material saved = materialService.createMaterial(request);
        return ResponseEntity.status(201).body(new ApiResponse<>(true, "Material created", saved));
    }

    // MaterialController.java

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Material>> updateMaterial(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "file", required = false) MultipartFile file, // optional
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "courseId", required = false) Long courseId
    ) {
        MaterialRequest req = new MaterialRequest();
        req.setTitle(title);
        req.setDescription(description);
        req.setTags(tags);
        req.setType(type);
        req.setCourseId(courseId);

        Material updated = materialService.updateMaterial(id, req, file);

        return ResponseEntity.ok(new ApiResponse<>(true, "Material updated", updated));
    }


    // existing delete (soft)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        materialService.deleteMaterial(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Material deleted", null));
    }

    // existing get by id
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Material>> getById(@PathVariable Long id) {
        Material material = materialService.getMaterialById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Material fetched", material));
    }

    // existing get all
    @GetMapping
    public ResponseEntity<ApiResponse<List<Material>>> getAll() {
        List<Material> list = materialService.getAllMaterials();
        return ResponseEntity.ok(new ApiResponse<>(true, "Materials fetched", list));
    }

    // existing get by uploader
    @GetMapping("/uploader/{uploaderId}")
    public ResponseEntity<ApiResponse<List<Material>>> getByUploader(@PathVariable Long uploaderId) {
        List<Material> list = materialService.getMaterialsByUploader(uploaderId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Materials fetched", list));
    }

    // existing search
//    @GetMapping("/search")
//    public ResponseEntity<ApiResponse<List<Material>>> searchByTag(@RequestParam String keyword) {
//        List<Material> list = materialService.searchMaterialsByTag(keyword);
//        return ResponseEntity.ok(new ApiResponse<>(true, "Materials fetched", list));
//    }
    // REPLACE the existing @GetMapping("/search") method with this:

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Material>>> searchMaterials(
            @RequestParam(required = false, defaultValue = "") String keyword) {
        return Optional.of(keyword)
                .map(materialService::searchMaterials)
                .map(results -> ResponseEntity.ok(
                        new ApiResponse<>(true, "Materials fetched", results)))
                .orElseGet(() -> ResponseEntity.ok(
                        new ApiResponse<>(true, "No materials found", Collections.emptyList())));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<List<Material>>> getMaterialsByCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Materials fetched", materialService.getMaterialsByCourse(courseId))
        );

    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MaterialResponse>> uploadMaterial(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam("uploadedBy") Long uploadedBy,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "courseId", required = false) Long courseId   // ✅ add this
    ) throws IOException {


        // 1) store file in local folder and get relative path: e.g. videos/uuid_name.mp4
        String relativePath = fileStorageService.store(file, type);

        // 2) create MaterialRequest and persist metadata
        MaterialRequest req = new MaterialRequest();
        req.setTitle(title);
        req.setDescription(description);
        req.setFileUrl(relativePath);
        req.setTags(tags);
        req.setUploadedBy(uploadedBy);
        req.setType(type);              // ✅ Save type
        req.setCourseId(courseId);      // ✅ Save courseId

        Material saved = materialService.createMaterial(req);

        // 3) build a friendly download URL for response (not stored in DB)
        String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/materials/download/")
                .path(saved.getId().toString())
                .toUriString();

        MaterialResponse resp = MaterialResponse.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .description(saved.getDescription())
                .fileUrl(downloadUrl)
                .tags(saved.getTags())
                .uploadedBy(saved.getUploadedBy())
                .type(saved.getType())              // ✅ return type
                .courseId(saved.getCourseId())      // ✅ return courseId
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, "Material uploaded", resp));
    }

    //download/stream file by material id
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Material material = materialService.getMaterialById(id);

        // material.getFileUrl() must be a relative path produced by upload (folder/filename)
        String relativePath = material.getFileUrl();
        Resource resource = fileStorageService.loadAsResource(relativePath);

        String filename = Paths.get(relativePath).getFileName().toString();
        MediaType mediaType = MediaTypeFactory.getMediaType(filename).orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

}
