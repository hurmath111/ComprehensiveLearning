package com.javis.ComprehensiveLearning.primaryService;

import com.javis.ComprehensiveLearning.model.Enrollment;
import com.javis.ComprehensiveLearning.repository.EnrollmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EnrollmentPrimaryService {

    @Autowired
    private EnrollmentRepository courseEnrollmentRepository;

    public boolean existsByUserIdAndCourseId(Long userId, Long courseId) {
        return courseEnrollmentRepository.existsByUserIdAndCourseId(userId, courseId);
    }

    public void saveAll(List<Enrollment> enrollments) {
        courseEnrollmentRepository.saveAll(enrollments);
    }

    public List<Enrollment> findByUserId(Long userId) {
        return courseEnrollmentRepository.findByUserId(userId);
    }

    public List<Enrollment> findByCourseIds(List<Long> courseIds) {
        return courseEnrollmentRepository.findByCourseIdIn(courseIds);
    }

    public List<Enrollment> findByCourseId(Long courseId) {
        return courseEnrollmentRepository.findByCourseId(courseId);
    }

    public void deleteByCourseIdIn(List<Long> courseIds) {
        courseEnrollmentRepository.deleteByCourseIdIn(courseIds);
    }
}
