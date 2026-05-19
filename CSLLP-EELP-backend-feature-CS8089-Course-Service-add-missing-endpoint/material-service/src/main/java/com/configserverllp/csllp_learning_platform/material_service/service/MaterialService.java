package com.configserverllp.csllp_learning_platform.material_service.service;

import com.configserverllp.csllp_learning_platform.material_service.dto.MaterialRequest;
import com.configserverllp.csllp_learning_platform.material_service.entity.Material;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MaterialService {
    Material createMaterial(MaterialRequest request);
    Material updateMaterial(Long id, MaterialRequest request, MultipartFile file);
    void deleteMaterial(Long id);
    Material getMaterialById(Long id);
    List<Material> getAllMaterials();
    List<Material> getMaterialsByUploader(Long uploaderId);
    List<Material> searchMaterialsByTag(String keyword);

    // file related
    String storeFile(MultipartFile file, String type);
    Resource loadFileAsResource(String relativePath);
    List<Material> getMaterialsByCourse(Long courseId);

    // ADD one line after searchMaterialsByTag declaration:
    List<Material> searchMaterials(String keyword);


}
