    package com.configserverllp.course_service.entity;

    import com.fasterxml.jackson.annotation.JsonProperty;
    import jakarta.persistence.*;
    import lombok.*;

    import java.time.LocalDateTime;
    import java.util.List;

    @Entity
    @Table(name = "courses")
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public class Course {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false)
        private String title;

        private String description;

        private String category;

        private int durationHours;

        @Column(name = "is_paid", nullable = false)   // ✅ fix here
        private boolean paid;

        private Double price;

        @Enumerated(EnumType.STRING)
        private Status status;

        private Long createdBy;

        private LocalDateTime createdAt;

        private LocalDateTime updatedAt;

        // ✅ New field: store material IDs locally
        @ElementCollection
        @CollectionTable(name = "course_materials", joinColumns = @JoinColumn(name = "course_id"))
        @Column(name = "material_id")
        private List<Long> materialIds;

        public enum Status {
            ACTIVE, INACTIVE

        }
        // ✅ FIXED: Change field name to avoid conflicts
        @Column(name = "is_mandatory", nullable = false)
        private boolean mandatory = false;

        // ✅ ADD: Custom getter for JSON serialization
        @JsonProperty("isMandatory")
        public boolean isMandatory() {
            return mandatory;
        }

        // ✅ ADD: Custom setter for JSON deserialization
        public void setMandatory(boolean mandatory) {
            this.mandatory = mandatory;
        }
    }
