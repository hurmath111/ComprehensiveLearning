package com.javis.ComprehensiveLearning.primaryService;

import com.javis.ComprehensiveLearning.model.Course;
import com.javis.ComprehensiveLearning.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CoursePrimaryService {
    @Autowired
    private CourseRepository courseRepository;

    public List<Course> findByTitleAndCategoryIn(List<Object[]> titleCategoryPairs) {
        return courseRepository.findByTitleAndCategoryIn(titleCategoryPairs);
    }

    public List<Course> findByCourseIds(List<Long> courseIds) {
        return courseRepository.findByCourseIdIn(courseIds);
    }
}
