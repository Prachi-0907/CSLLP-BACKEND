    package com.configserverllp.csllp_learning_platform.material_service.repository;
    import com.configserverllp.csllp_learning_platform.material_service.entity.Material;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.data.jpa.repository.Query;
    import org.springframework.data.repository.query.Param;
    import org.springframework.stereotype.Repository;

    import java.util.List;

    @Repository
    public interface MaterialRepository extends JpaRepository<Material, Long> {
        List<Material> findByUploadedBy(Long uploadedBy);
        List<Material> findByStatus(Material.Status status);
        List<Material> findByTagsContainingIgnoreCase(String keyword);
        List<Material> findByCourseId(Long courseId);

        // ADD this method to MaterialRepository.java — no change to existing methods

        @Query("SELECT m FROM Material m WHERE m.status = " +
                "com.configserverllp.csllp_learning_platform.material_service.entity.Material.Status.ACTIVE " +
                "AND (LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                "OR LOWER(COALESCE(m.tags, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                "OR LOWER(COALESCE(m.type, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        List<Material> searchByKeyword(@Param("keyword") String keyword);


    }
