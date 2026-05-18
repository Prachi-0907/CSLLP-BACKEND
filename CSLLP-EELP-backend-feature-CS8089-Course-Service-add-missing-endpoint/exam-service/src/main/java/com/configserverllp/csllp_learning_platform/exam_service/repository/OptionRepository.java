package com.configserverllp.csllp_learning_platform.exam_service.repository;

import com.configserverllp.csllp_learning_platform.exam_service.entity.Option;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OptionRepository extends JpaRepository<Option, Long> {
    List<Option> findByQuestionId(Long questionId);
}
