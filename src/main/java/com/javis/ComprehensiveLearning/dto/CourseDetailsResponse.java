package com.javis.ComprehensiveLearning.dto;

import com.javis.ComprehensiveLearning.constants.EnrollmentStatusEnum;
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
    private String courseTitle;
    private String category;
    private String description;
    private String syllabus;
    private String duration;
    private String instructor;
    private EnrollmentStatusEnum status;
}
