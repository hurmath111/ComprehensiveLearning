package com.javis.ComprehensiveLearning.controller;

import com.javis.ComprehensiveLearning.dto.CourseRequest;
import com.javis.ComprehensiveLearning.dto.CreateUpdateRequest;
import com.javis.ComprehensiveLearning.model.Course;
import com.javis.ComprehensiveLearning.security.TokenProvider;
import com.javis.ComprehensiveLearning.service.CourseManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class CourseManagementController {

    @Autowired
    private CourseManagementService courseManagementService;

    @Autowired
    private TokenProvider tokenProvider;

    @PostMapping("/create-update")
    public ResponseEntity<?> createOrUpdateCourse(@RequestBody List<CreateUpdateRequest> createUpdateRequests) {
        try{
            List<Course> courses = courseManagementService.createOrUpdateCourses(createUpdateRequests);
            return ResponseEntity.ok(courses);
        }catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteCourses(@RequestBody List<CourseRequest> courseRequests) {
        try{
            courseManagementService.deleteCourses(courseRequests);
            return ResponseEntity.ok("Course Deleted");
        }catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/fetch")
    public ResponseEntity<?> fetchCourseDetails(@RequestBody List<CourseRequest> courseRequests) {
        List<Course> courses = courseManagementService.fetchCourseDetails(courseRequests);
        return ResponseEntity.ok(courses);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadCourseFile(@RequestParam("file") MultipartFile file) {
        try{
            List<Course> courseDetailsList = courseManagementService.uploadCourseFile(file);
            return ResponseEntity.ok(courseDetailsList);
        }catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping(value = "/upload-syllabus", consumes = {"multipart/form-data"})
    public ResponseEntity<?> uploadPdfFile(@RequestPart("courseId") Long courseId,
                                           @RequestPart("document") MultipartFile file) {
        try {
            String response = courseManagementService.uploadPdfFile(file, courseId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
