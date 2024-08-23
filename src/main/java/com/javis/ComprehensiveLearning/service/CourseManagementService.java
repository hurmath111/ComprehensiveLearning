package com.javis.ComprehensiveLearning.service;

import com.javis.ComprehensiveLearning.constants.ErrorMessageEnum;
import com.javis.ComprehensiveLearning.constants.RoleEnum;
import com.javis.ComprehensiveLearning.dto.CourseRequest;
import com.javis.ComprehensiveLearning.dto.CreateUpdateRequest;
import com.javis.ComprehensiveLearning.model.Course;
import com.javis.ComprehensiveLearning.model.Enrollment;
import com.javis.ComprehensiveLearning.model.User;
import com.javis.ComprehensiveLearning.primaryService.CoursePrimaryService;
import com.javis.ComprehensiveLearning.primaryService.EnrollmentPrimaryService;
import com.javis.ComprehensiveLearning.primaryService.UserPrimaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CourseManagementService {

    @Autowired
    private CoursePrimaryService coursePrimaryService;

    @Autowired
    private EnrollmentPrimaryService enrollmentPrimaryService;

    @Autowired
    private UserPrimaryService userPrimaryService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private S3Service s3Service;

    public List<Course> createOrUpdateCourses(List<CreateUpdateRequest> createUpdateRequests) throws Exception {
        // Extract title-category pairs from the requests
        List<Object[]> titleCategoryPairs = createUpdateRequests.stream()
                .map(req -> new Object[]{req.getTitle(), req.getCategory()})
                .collect(Collectors.toList());

        // Fetch existing courses in a single DB call
        List<Course> existingCourses = coursePrimaryService.findByTitleAndCategoryIn(titleCategoryPairs);

        // Mapping existing courses by title-category for quick lookup
        Map<String, Course> existingCourseMap = existingCourses.stream()
                .collect(Collectors.toMap(
                        course -> course.getCourseTitle() + ":" + course.getCategory(),
                        course -> course
                ));

        // Fetch all users in a single DB call
        List<User> allUsers = userPrimaryService.findAllByRole(RoleEnum.USER);

        if(allUsers.isEmpty()) {
            throw new IllegalArgumentException(ErrorMessageEnum.NO_USERS.getMessage());
        }

        Map<Long, User> userIdToUserMap = allUsers.stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));

        // Fetch all enrollments for the existing courses
        List<Long> courseIds = existingCourses.stream()
                .map(Course::getCourseId)
                .collect(Collectors.toList());
        List<Enrollment> enrolledCourses = enrollmentPrimaryService.findByCourseIds(courseIds);

        // Creating a map of course IDs to enrolled users
        Map<Long, List<User>> enrolledcourseIdToUsersMap = enrolledCourses.stream()
                .collect(Collectors.groupingBy(
                        Enrollment::getCourseId,
                        Collectors.mapping(
                                enrollment -> userIdToUserMap.get(enrollment.getUserId()),
                                Collectors.toList()
                        )
                ));

        // Process all courses (create/update)
        List<Course> coursesToSave = createUpdateRequests.stream()
                .map(req -> {
                    String key = req.getTitle() + ":" + req.getCategory();
                    Course course = existingCourseMap.getOrDefault(key, Course.builder().syllabus(null).build());

                    course.setCourseTitle(req.getTitle());
                    course.setCategory(req.getCategory());
                    course.setDescription(req.getDescription());
                    course.setDuration(req.getDuration());
                    course.setInstructor(req.getInstructor());

                    Long courseId = course.getCourseId();

                    if (courseId != null && enrolledcourseIdToUsersMap.containsKey(courseId)) {
                        // Notify enrolled users for course updates
                        List<User> enrolledUsers = enrolledcourseIdToUsersMap.get(courseId);
                        notifyEnrolledUsersForCourseUpdate(course, enrolledUsers);
                    } else {
                        // Notify all users for new courses
                        notifyAllUsersForNewCourse(course, allUsers);
                    }

                    return course;
                })
                .collect(Collectors.toList());

        // Save all courses in a single DB call
        return coursePrimaryService.saveAll(coursesToSave);
    }

    public void deleteCourse(CourseRequest courseRequest) {
        List<Object[]> titleCategoryPairs = new ArrayList<>();
        titleCategoryPairs.add(new Object[]{courseRequest.getCourseTitle(), courseRequest.getCategory()});


        List<Course> courses = coursePrimaryService.findByTitleAndCategoryIn(titleCategoryPairs);

        if (courses.isEmpty()) {
            throw new IllegalArgumentException(ErrorMessageEnum.NO_COURSE.getMessage());
        }

        List<User> allUsers = userPrimaryService.findAllByRole(RoleEnum.USER);

        if(allUsers.isEmpty()) {
            throw new IllegalArgumentException(ErrorMessageEnum.NO_USERS.getMessage());
        }

        coursePrimaryService.deleteAll(courses);

        notifyAllUsersForDeletedCourse(courses.get(0), allUsers);
    }

    public Course fetchCourseDetails(CourseRequest courseRequest) {
        List<Object[]> titleCategoryPairs = new ArrayList<>();
        titleCategoryPairs.add(new Object[]{courseRequest.getCourseTitle(), courseRequest.getCategory()});


        List<Course> courses = coursePrimaryService.findByTitleAndCategoryIn(titleCategoryPairs);

        if (courses.isEmpty()) {
            throw new IllegalArgumentException(ErrorMessageEnum.NO_COURSE.getMessage());
        }

        return courses.get(0);
    }

    public String uploadPdfFile(MultipartFile file) {

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.lastIndexOf(".") == -1) {
            throw new IllegalArgumentException(ErrorMessageEnum.NO_DOC.getMessage());
        }
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toLowerCase();

        if (!"pdf".equals(fileExtension)) {
            throw new IllegalArgumentException(ErrorMessageEnum.INVALID_DOC_TYPE.getMessage());
        }
        String documentLink = uploadDocumentToS3(file);

        return "PDF uploaded successfully";
    }

    private String uploadDocumentToS3(MultipartFile file) {
        return s3Service.uploadDocument(file);//please check the credentials mentioned in application properties
        //return "aiwguyg.pdf";
    }

    private void notifyEnrolledUsersForCourseUpdate(Course course, List<User> enrolledUsers) {
        String subject = "Course Update Notification";
        String text = "The course " + course.getCourseTitle() + " of the " + course.getCategory() + " category has been updated.";
        for (User user : enrolledUsers) {
            if (user != null) {
                emailService.sendEmail(user.getEmail(), subject, text);
            }
        }
    }

    private void notifyAllUsersForNewCourse(Course course, List<User> allUsers) {
        String subject = "New Course Available!";
        String text = "A new course " + course.getCourseTitle() + " of the " + course.getCategory() + " category has been added.";
        for (User user : allUsers) {
            if (user != null) {
                emailService.sendEmail(user.getEmail(), subject, text);
            }
        }
    }

    private void notifyAllUsersForDeletedCourse(Course course, List<User> allUsers) {
        String subject = "Course Deleted!";
        String text = "The course " + course.getCourseTitle() + " of the " + course.getCategory() + " category has been removed.";
        for (User user : allUsers) {
            if (user != null) {
                emailService.sendEmail(user.getEmail(), subject, text);
            }
        }
    }
}
