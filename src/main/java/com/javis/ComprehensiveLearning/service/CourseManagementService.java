package com.javis.ComprehensiveLearning.service;

import com.javis.ComprehensiveLearning.constants.ErrorMessageEnum;
import com.javis.ComprehensiveLearning.constants.RoleEnum;
import com.javis.ComprehensiveLearning.dto.CourseRequest;
import com.javis.ComprehensiveLearning.dto.CourseTitleAndCategory;
import com.javis.ComprehensiveLearning.dto.CreateUpdateRequest;
import com.javis.ComprehensiveLearning.model.Course;
import com.javis.ComprehensiveLearning.model.Enrollment;
import com.javis.ComprehensiveLearning.model.User;
import com.javis.ComprehensiveLearning.primaryService.CoursePrimaryService;
import com.javis.ComprehensiveLearning.primaryService.EnrollmentPrimaryService;
import com.javis.ComprehensiveLearning.primaryService.UserPrimaryService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    public List<Course> createOrUpdateCourses(List<CreateUpdateRequest> createUpdateRequests) {

        //Filter the courses to only include those with matching title-category pairs
        List<Course> existingCourses = filterAndFetchCourses(createUpdateRequests);

        // Fetch all users in a single DB call
        List<User> allUsers = userPrimaryService.findAllByRole(RoleEnum.USER);

/*        if(allUsers.isEmpty()) {
            throw new IllegalArgumentException(ErrorMessageEnum.NO_USERS.getMessage());
        }*/

        return createOrUpdateProcess(createUpdateRequests, existingCourses, allUsers);
    }

    public void deleteCourses(List<CourseRequest> courseRequests) {
        List<Course> courses = filterAndFetchCourses(courseRequests);

        if (courses.isEmpty()) {
            throw new IllegalArgumentException(ErrorMessageEnum.NO_COURSE.getMessage());
        }

        coursePrimaryService.deleteAll(courses);

        List<User> allUsers = userPrimaryService.findAllByRole(RoleEnum.USER);

        if(allUsers.isEmpty()) {
            throw new IllegalArgumentException(ErrorMessageEnum.NO_USERS.getMessage());
        }

        for (Course course : courses) {
            notifyAllUsersForDeletedCourse(course, allUsers);
        }
    }

    public List<Course> fetchCourseDetails(List<CourseRequest> courseRequests) {
        List<Course> courses = filterAndFetchCourses(courseRequests);

        if (courses.isEmpty()) {
            throw new IllegalArgumentException(ErrorMessageEnum.NO_COURSE.getMessage());
        }

        return courses;
    }

    public List<Course> uploadCourseFile(MultipartFile file) throws Exception {

        List<User> allUsers = userPrimaryService.findAllByRole(RoleEnum.USER);

        //Extracting data from Excel file
        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);

        List<CreateUpdateRequest> courseRequests = new ArrayList<>();

        for (Row row : sheet) {
            if (row.getRowNum() == 0) {
                continue; // Skip header row
            }
            CreateUpdateRequest request = new CreateUpdateRequest();
            request.setCourseTitle(row.getCell(0).getStringCellValue());
            request.setCategory(row.getCell(1).getStringCellValue());
            request.setDescription(row.getCell(2).getStringCellValue());
            request.setDuration(row.getCell(4).getStringCellValue());
            request.setInstructor(row.getCell(5).getStringCellValue());

            courseRequests.add(request);
        }

        workbook.close();

        List<Course> allCourses = coursePrimaryService.findAll();

        Map<String, CreateUpdateRequest> requestMap = courseRequests.stream()
                .collect(Collectors.toMap(
                        req -> req.getCourseTitle() + ":" + req.getCategory(),
                        req -> req
                ));

        // Filtering existing courses based on title and category matching the requests
        List<Course> existingCourses = allCourses.stream()
                .filter(course -> requestMap.containsKey(course.getCourseTitle() + ":" + course.getCategory()))
                .toList();

        // Filtering courses to delete as those that do not match any title-category in the requests
        List<Course> toDeleteCourses = allCourses.stream()
                .filter(course -> !requestMap.containsKey(course.getCourseTitle() + ":" + course.getCategory()))
                .toList();

        if (!toDeleteCourses.isEmpty()) {
            coursePrimaryService.deleteAll(toDeleteCourses);
            for (Course course : toDeleteCourses) {
                notifyAllUsersForDeletedCourse(course, allUsers);
            }
        }

        //Calling the createOrUpdateCourses method
        return createOrUpdateProcess(courseRequests, existingCourses, allUsers);
    }

    public String uploadPdfFile(MultipartFile file, Long courseId) {

        Course course = coursePrimaryService.findByCourseId(courseId);
        if(course == null) {
            throw new IllegalArgumentException(ErrorMessageEnum.NO_COURSE.getMessage());
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.lastIndexOf(".") == -1) {
            throw new IllegalArgumentException(ErrorMessageEnum.NO_DOC.getMessage());
        }
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toLowerCase();

        if (!"pdf".equals(fileExtension)) {
            throw new IllegalArgumentException(ErrorMessageEnum.INVALID_DOC_TYPE.getMessage());
        }
        String documentLink = uploadDocumentToS3(file);

        course.setSyllabus(documentLink);

        coursePrimaryService.save(course);

        return "PDF uploaded successfully";
    }

    private List<Course> createOrUpdateProcess(List<CreateUpdateRequest> createUpdateRequests, List<Course> existingCourses, List<User> allUsers) {

        // Mapping existing courses by title-category for quick lookup
        Map<String, Course> existingCourseMap = existingCourses.stream()
                .collect(Collectors.toMap(
                        course -> course.getCourseTitle() + ":" + course.getCategory(),
                        course -> course
                ));

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
                    String key = req.getCourseTitle() + ":" + req.getCategory();
                    Course course = existingCourseMap.getOrDefault(key, Course.builder().syllabus(null).build());

                    course.setCourseTitle(req.getCourseTitle());
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

    private <T extends CourseTitleAndCategory> List<Course> filterAndFetchCourses(List<T> courseRequests) {
        List<String> courseTitles = courseRequests.stream()
                .map(CourseTitleAndCategory::getCourseTitle)
                .distinct()
                .collect(Collectors.toList());

        List<String> categories = courseRequests.stream()
                .map(CourseTitleAndCategory::getCategory)
                .distinct()
                .collect(Collectors.toList());

        List<Course> allMatchingCourses = coursePrimaryService.findByCourseTitleInAndCategoryIn(courseTitles, categories);

        Set<String> validPairs = courseRequests.stream()
                .map(req -> req.getCourseTitle() + ":" + req.getCategory())
                .collect(Collectors.toSet());

        return allMatchingCourses.stream()
                .filter(course -> validPairs.contains(course.getCourseTitle() + ":" + course.getCategory()))
                .collect(Collectors.toList());
    }

    private String uploadDocumentToS3(MultipartFile file) {
        return s3Service.uploadDocument(file);//please check the credentials mentioned in application properties
        //return "aiwguyg.pdf";
    }

    private void notifyUsersForCourse(Course course, List<User> users, String subject, String text) {
        for (User user : users) {
            if (user != null) {
                emailService.sendEmail(user.getEmail(), subject, text);
            }
        }
    }

    private void notifyEnrolledUsersForCourseUpdate(Course course, List<User> enrolledUsers) {
        String subject = "Course Update Notification";
        String text = "The course " + course.getCourseTitle() + " of the " + course.getCategory() + " category has been updated.";
        notifyUsersForCourse(course, enrolledUsers, subject, text);
    }

    private void notifyAllUsersForNewCourse(Course course, List<User> allUsers) {
        String subject = "New Course Available!";
        String text = "A new course " + course.getCourseTitle() + " of the " + course.getCategory() + " category has been added.";
        notifyUsersForCourse(course, allUsers, subject, text);
    }

    private void notifyAllUsersForDeletedCourse(Course course, List<User> allUsers) {
        String subject = "Course Deleted!";
        String text = "The course " + course.getCourseTitle() + " of the " + course.getCategory() + " category has been removed.";
        notifyUsersForCourse(course, allUsers, subject, text);
    }
}
