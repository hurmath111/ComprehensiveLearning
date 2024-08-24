package com.javis.ComprehensiveLearning.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseRequest implements CourseTitleAndCategory{
    private String courseTitle;
    private String category;
}
