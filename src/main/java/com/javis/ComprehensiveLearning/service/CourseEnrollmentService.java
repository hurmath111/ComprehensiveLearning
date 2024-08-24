package com.javis.ComprehensiveLearning.service;

import com.javis.ComprehensiveLearning.constants.EnrollmentStatusEnum;
import com.javis.ComprehensiveLearning.dto.CourseRequest;
import com.javis.ComprehensiveLearning.dto.EnrollmentResponse;
import com.javis.ComprehensiveLearning.model.Course;
import com.javis.ComprehensiveLearning.model.Enrollment;
import com.javis.ComprehensiveLearning.primaryService.CoursePrimaryService;
import com.javis.ComprehensiveLearning.primaryService.EnrollmentPrimaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CourseEnrollmentService {

    @Autowired
    private CoursePrimaryService coursePrimaryService;

    @Autowired
    private EnrollmentPrimaryService enrollmentPrimaryService;

    public List<EnrollmentResponse> enrollCourses(Long userId, List<CourseRequest> enrollmentRequests) {

        List<EnrollmentResponse> enrollmentResponses = enrollmentRequests.stream()
                .map(request -> new EnrollmentResponse(request.getCourseTitle(), request.getCategory(), EnrollmentStatusEnum.NOT_AVAILABLE))
                .toList();

        List<String> courseTitles = enrollmentRequests.stream()
                .map(CourseRequest::getCourseTitle)
                .distinct()
                .collect(Collectors.toList());

        List<String> categories = enrollmentRequests.stream()
                .map(CourseRequest::getCategory)
                .distinct()
                .collect(Collectors.toList());

        // Fetching matching courses in a single DB call
        List<Course> allMatchingCourses = coursePrimaryService.findByCourseTitleInAndCategoryIn(courseTitles, categories);

        Set<String> validPairs = enrollmentRequests.stream()
                .map(req -> req.getCourseTitle() + ":" + req.getCategory())
                .collect(Collectors.toSet());

        // Step 4: Filter the courses to only include those with matching title-category pairs
        List<Course> existingCourses = allMatchingCourses.stream()
                .filter(course -> validPairs.contains(course.getCourseTitle() + ":" + course.getCategory()))
                .toList();

        if (existingCourses.isEmpty()) {
            return enrollmentResponses;
        }

        Map<String, Course> courseMap = existingCourses.stream()
                .collect(Collectors.toMap(
                        course -> course.getCourseTitle() + "_" + course.getCategory(),
                        course -> course
                ));

        Map<Long, Enrollment> existingEnrollments = enrollmentPrimaryService.findByUserId(userId).stream()
                .collect(Collectors.toMap(Enrollment::getCourseId, enrollment -> enrollment));

        List<Enrollment> enrollments = new ArrayList<>();

        for (EnrollmentResponse response : enrollmentResponses) {
            Course course = courseMap.get(response.getCourseTitle() + "_" + response.getCategory());
            if (course != null) {
                EnrollmentStatusEnum newStatus = course.getSyllabus() != null ? EnrollmentStatusEnum.AVAILABLE : EnrollmentStatusEnum.AWAITING;
                Enrollment existingEnrollment = existingEnrollments.get(course.getCourseId());
                if (existingEnrollment == null) {
                    // Add to new enrollments if not already enrolled
                    Enrollment enrollment = Enrollment.builder().userId(userId).courseId(course.getCourseId()).status(newStatus).build();
                    enrollments.add(enrollment);

                } else if (!existingEnrollment.getStatus().equals(newStatus)) {
                    // Update status if different from the existing status
                    existingEnrollment.setStatus(newStatus);
                    enrollments.add(existingEnrollment);
                }
                response.setStatus(newStatus);
            }
        }

        if (!enrollments.isEmpty()) {
            enrollmentPrimaryService.saveAll(enrollments);
        }

        return enrollmentResponses;
    }

    public List<Course> getAllEnrolledCourses(Long userId) {
        List<Enrollment> enrollments = enrollmentPrimaryService.findByUserId(userId);

        if (enrollments.isEmpty()) {
            return Collections.emptyList(); //TODO:correct response
        }

        // Extracting course IDs from the enrollments
        List<Long> courseIds = enrollments.stream()
                .map(Enrollment::getCourseId)
                .collect(Collectors.toList());

        // Fetching complete course details in one DB call
        return coursePrimaryService.findByCourseIds(courseIds);
    }
}
