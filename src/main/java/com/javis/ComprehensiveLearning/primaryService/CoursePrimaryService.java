package com.javis.ComprehensiveLearning.primaryService;

import com.javis.ComprehensiveLearning.model.Course;
import com.javis.ComprehensiveLearning.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class CoursePrimaryService {
    @Autowired
    private CourseRepository courseRepository;

    public List<Course> findByCourseTitleInAndCategoryIn(List<String> courseTitles, List<String> categories) {
        return courseRepository.findByCourseTitleInAndCategoryIn(courseTitles, categories);
    }

    public List<Course> findByCourseIds(List<Long> courseIds) {
        return courseRepository.findByCourseIdIn(courseIds);
    }

    public Course findByCourseId(Long courseId) {
        return courseRepository.findByCourseId(courseId);
    }

    public List<Course> saveAll(List<Course> courses) {
        return courseRepository.saveAll(courses);
    }

    public void deleteAll(List<Course> courses) {
        courseRepository.deleteAll(courses);
    }

    public List<Course> findAll() {
        return courseRepository.findAll();
    }

    public void save(Course course) {
        courseRepository.save(course);
    }

    public List<Course> findAllByCategoryIn(Set<String> categories) {
        return courseRepository.findByCategoryIn(categories);
    }
}
