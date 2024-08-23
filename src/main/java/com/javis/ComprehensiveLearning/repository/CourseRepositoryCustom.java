package com.javis.ComprehensiveLearning.repository;

import com.javis.ComprehensiveLearning.model.Course;

import java.util.List;

public interface CourseRepositoryCustom {
    List<Course> findByTitleAndCategoryIn(List<Object[]> titleCategoryPairs);
}
