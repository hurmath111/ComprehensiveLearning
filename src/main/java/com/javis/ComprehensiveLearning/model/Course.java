package com.javis.ComprehensiveLearning.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Course")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Course_Id")
    private Long courseId;

    @Column(name = "CourseTitle", nullable = false)
    private String courseTitle;//TODO:make title+category as unique,some function

    @Column(name = "Category", nullable = false)
    private String category;

    @Column(name = "Description")
    private String description;

    @Column(name = "Syllabus")
    private String syllabus;

    @Column(name = "Duration")
    private String duration;

    @Column(name = "Instructor", nullable = false)
    private String instructor;
}
