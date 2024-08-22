package com.javis.ComprehensiveLearning.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseDetailsResponse {
    private Long courseId;
    private String title;
    private String category;
    private String description;
    private String syllabus;
    private String duration;
    private String instructor;
    private String status;
}
