package com.configserverllp.course_service.repository;

import com.configserverllp.course_service.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByStatus(Course.Status status);
    List<Course> findByCategoryAndStatus(String category, Course.Status status);

    //Search by title or description
    @Query("SELECT c FROM Course c WHERE (LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND c.status = 'ACTIVE'")
    List<Course> searchByKeyword(String keyword);

    // Get courses created by specific user
    List<Course> findByCreatedBy(Long createdBy);
    // ADD THESE NEW METHODS:
    Optional<Course> findByTitle(String title);
    List<Course> findByTitleContainingIgnoreCase(String title);
}
