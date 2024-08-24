package com.javis.ComprehensiveLearning.controller;

import com.javis.ComprehensiveLearning.dto.CourseRequest;
import com.javis.ComprehensiveLearning.dto.EnrollmentResponse;
import com.javis.ComprehensiveLearning.model.Course;
import com.javis.ComprehensiveLearning.security.TokenProvider;
import com.javis.ComprehensiveLearning.service.CourseEnrollmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
public class CourseEnrollmentController {

    @Autowired
    private CourseEnrollmentService courseEnrollmentService;

    @Autowired
    private TokenProvider tokenProvider;

    @PostMapping("/enroll")
    public ResponseEntity<?> enrollCourses(
            @RequestBody List<CourseRequest> enrollmentRequests,
            @RequestHeader("Authorization") String authorizationHeader) {

        try{
            String token = authorizationHeader.replace("Bearer ", "");
            Long userId = Long.parseLong(tokenProvider.getUserIdFromToken(token));

            List<EnrollmentResponse> enrollmentResponses = courseEnrollmentService.enrollCourses(userId, enrollmentRequests);

            return ResponseEntity.ok(enrollmentResponses);
        }catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllEnrolledCourses(@RequestHeader("Authorization") String authorizationHeader) {

        try{
            String token = authorizationHeader.replace("Bearer ", "");
            Long userId = Long.parseLong(tokenProvider.getUserIdFromToken(token));

            List<Course> enrolledCourses = courseEnrollmentService.getAllEnrolledCourses(userId);

            return ResponseEntity.ok(enrolledCourses);
        }catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
