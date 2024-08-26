package com.javis.ComprehensiveLearning.repository;

import com.javis.ComprehensiveLearning.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long>{
    List<Course> findByCourseIdIn(List<Long> courseIds);
    Course findByCourseId(Long courseId);
    List<Course> findByCourseTitleInAndCategoryIn(List<String> courseTitles, List<String> categories);
    List<Course> findByCategoryIn(Set<String> categories);
}