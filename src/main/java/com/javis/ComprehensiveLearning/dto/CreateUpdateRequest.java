package com.javis.ComprehensiveLearning.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateUpdateRequest {
    private String title;
    private String category;
    private String description;
    private String duration;
    private String instructor;
}
