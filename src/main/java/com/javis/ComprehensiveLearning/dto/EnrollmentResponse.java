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
public class EnrollmentResponse {
    private String courseTitle;
    private String category;
    private EnrollmentStatusEnum status;
}
