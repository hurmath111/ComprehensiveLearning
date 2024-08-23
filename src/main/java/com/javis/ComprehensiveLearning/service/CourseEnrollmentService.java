package com.javis.ComprehensiveLearning.service;

import com.javis.ComprehensiveLearning.constants.EnrollmentStatusEnum;
import com.javis.ComprehensiveLearning.dto.CourseDetailsResponse;
import com.javis.ComprehensiveLearning.dto.CourseRequest;
import com.javis.ComprehensiveLearning.dto.EnrollmentResponse;
import com.javis.ComprehensiveLearning.model.Course;
import com.javis.ComprehensiveLearning.model.Enrollment;
import com.javis.ComprehensiveLearning.primaryService.CoursePrimaryService;
import com.javis.ComprehensiveLearning.primaryService.EnrollmentPrimaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

        // Creating title-category pairs for the query
        List<Object[]> titleCategoryPairs = enrollmentRequests.stream()
                .map(request -> new Object[]{request.getCourseTitle(), request.getCategory()})
                .collect(Collectors.toList());

        // Fetching all matching courses in one DB call
        List<Course> matchingCourses = coursePrimaryService.findByTitleAndCategoryIn(titleCategoryPairs);//TODO:change the names of variables

        if (matchingCourses.isEmpty()) {
            return enrollmentResponses;
        }

        Map<String, Course> courseMap = matchingCourses.stream()
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

    public List<CourseDetailsResponse> getAllEnrolledCourses(Long userId) {
        List<Enrollment> enrollments = enrollmentPrimaryService.findByUserId(userId);

        if (enrollments.isEmpty()) {
            return Collections.emptyList(); //TODO:correct response
        }

        // Extracting course IDs from the enrollments
        List<Long> courseIds = enrollments.stream()
                .map(Enrollment::getCourseId)
                .collect(Collectors.toList());

        // Fetching complete course details in one DB call
        List<Course> courses = coursePrimaryService.findByCourseIds(courseIds);

        Map<Long, Course> courseMap = courses.stream()
                .collect(Collectors.toMap(Course::getCourseId, course -> course));

        return enrollments.stream()
                .map(enrollment -> {
                    Course course = courseMap.get(enrollment.getCourseId());
                    return new CourseDetailsResponse(
                            enrollment.getCourseId(),
                            course.getCourseTitle(),
                            course.getCategory(),
                            course.getDescription(),
                            course.getSyllabus(),
                            course.getDuration(),
                            course.getInstructor(),
                            enrollment.getStatus().name()
                    );
                })
                .collect(Collectors.toList());
    }
}
