package com.configserverllp.csllp_learning_platform.material_service.storage;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path basePath;

    public FileStorageService(@Value("${material.upload.base-dir:uploads/materials}") String uploadDir) {
        this.basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(basePath.resolve("videos"));
            Files.createDirectories(basePath.resolve("pdfs"));
            Files.createDirectories(basePath.resolve("documents"));
            Files.createDirectories(basePath.resolve("others"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize storage directories: " + e.getMessage(), e);
        }
    }

    /**
     * Store the incoming file to appropriate folder and return a relative path like: videos/uuid_filename.mp4
     */
    public String store(MultipartFile file, String explicitType) {
        String original = StringUtils.cleanPath(file.getOriginalFilename());
        if (original.contains("..")) {
            throw new RuntimeException("Invalid file name: " + original);
        }

        String ext = "";
        int idx = original.lastIndexOf('.');
        if (idx > 0) ext = original.substring(idx + 1).toLowerCase();

        String folder = determineFolder(explicitType, ext);
        String uniqueName = UUID.randomUUID().toString() + "_" + original;
        Path target = basePath.resolve(folder).resolve(uniqueName);

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            // we return a path relative to basePath, e.g. "videos/uuid_name.mp4"
            return folder + "/" + uniqueName;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store file " + original, ex);
        }
    }

    private String determineFolder(String explicitType, String ext) {
        if (explicitType != null) {
            String low = explicitType.toLowerCase();
            if (low.contains("video")) return "videos";
            if (low.contains("pdf")) return "pdfs";
            if (low.contains("doc") || low.contains("document") || low.contains("ppt")) return "documents";
        }
        if (ext.matches("mp4|mkv|mov|avi|webm")) return "videos";
        if (ext.matches("pdf")) return "pdfs";
        if (ext.matches("doc|docx|ppt|pptx|xls|xlsx|txt")) return "documents";
        return "others";
    }

    /**
     * Load the file as a Resource given a relative path like: videos/uuid_name.mp4
     */
    public Resource loadAsResource(String relativePath) {
        try {
            Path file = basePath.resolve(relativePath).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) return resource;
            else throw new RuntimeException("File not found: " + relativePath);
        } catch (MalformedURLException e) {
            throw new RuntimeException("File not found: " + relativePath, e);
        }
    }

    /**
     * Get the absolute Path for a relative path (helper if needed)
     */
    public Path resolvePath(String relativePath) {
        return basePath.resolve(relativePath).normalize();
    }
}
