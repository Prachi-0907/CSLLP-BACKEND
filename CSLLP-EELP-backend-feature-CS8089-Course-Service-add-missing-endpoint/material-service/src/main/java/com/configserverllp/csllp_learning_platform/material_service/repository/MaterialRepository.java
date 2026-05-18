    package com.configserverllp.csllp_learning_platform.material_service.repository;
    import com.configserverllp.csllp_learning_platform.material_service.entity.Material;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.stereotype.Repository;

    import java.util.List;

    @Repository
    public interface MaterialRepository extends JpaRepository<Material, Long> {
        List<Material> findByUploadedBy(Long uploadedBy);
        List<Material> findByStatus(Material.Status status);
        List<Material> findByTagsContainingIgnoreCase(String keyword);
        List<Material> findByCourseId(Long courseId);


    }
