package com.configserverllp.csllp_learning_platform.submission_service.controller;

import com.configserverllp.csllp_learning_platform.submission_service.dto.SubmissionDto;
import com.configserverllp.csllp_learning_platform.submission_service.service.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    @Autowired
    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping
    public ResponseEntity<SubmissionDto> createSubmission(@RequestBody SubmissionDto submissionDto) {
        return ResponseEntity.ok(submissionService.createSubmission(submissionDto));
    }

    @GetMapping
    public ResponseEntity<List<SubmissionDto>> getAllSubmissions() {
        return ResponseEntity.ok(submissionService.getAllSubmissions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubmissionDto> getSubmissionById(@PathVariable Long id) {
        return ResponseEntity.ok(submissionService.getSubmissionById(id));
    }

    @GetMapping("/exam/{examId}")
    public ResponseEntity<List<SubmissionDto>> getSubmissionsByExam(@PathVariable Long examId) {
        return ResponseEntity.ok(submissionService.getSubmissionsByExam(examId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SubmissionDto>> getSubmissionsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(submissionService.getSubmissionsByUser(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubmissionDto> updateSubmission(@PathVariable Long id, @RequestBody SubmissionDto submissionDto) {
        return ResponseEntity.ok(submissionService.updateSubmission(id, submissionDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubmission(@PathVariable Long id) {
        submissionService.deleteSubmission(id);
        return ResponseEntity.noContent().build();
    }
}
