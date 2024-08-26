package com.javis.ComprehensiveLearning.service;

import com.javis.ComprehensiveLearning.constants.CourseActionEnum;
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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Locale.filter;

@Service
public class CourseManagementService extends BaseCourseService {

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

    @Transactional
    public List<Course> createOrUpdateCourses(List<CreateUpdateRequest> createUpdateRequests) {

        //Filter the courses to only include those with matching title-category pairs
        List<Course> existingCourses = fetchAndFilterCourses(createUpdateRequests);

        // Fetch all users in a single DB call
        List<User> allUsers = userPrimaryService.findAllByRole(RoleEnum.USER);

/*        if(allUsers.isEmpty()) {
            throw new IllegalArgumentException(ErrorMessageEnum.NO_USERS.getMessage());
        }*/

        return createOrUpdateProcess(createUpdateRequests, existingCourses, allUsers);
    }

    @Transactional
    public void deleteCourses(List<CourseRequest> courseRequests) {
        List<Course> courses = fetchAndFilterCourses(courseRequests);

        if (courses.isEmpty()) {
            throw new IllegalArgumentException(ErrorMessageEnum.NO_COURSE.getMessage());
        }

        deleteEnrollmentsForCourses(courses);

        coursePrimaryService.deleteAll(courses);

        List<User> allUsers = userPrimaryService.findAllByRole(RoleEnum.USER);

        if(allUsers.isEmpty()) {
            throw new IllegalArgumentException(ErrorMessageEnum.NO_USERS.getMessage());
        }

        notifyUsersForCourseUpdate(courses, allUsers, CourseActionEnum.DELETED.name().toLowerCase(Locale.ROOT));
    }

    public List<Course> fetchCourseDetails(List<CourseRequest> courseRequests) {
        List<Course> courses = fetchAndFilterCourses(courseRequests);

        if (courses.isEmpty()) {
            throw new IllegalArgumentException(ErrorMessageEnum.NO_COURSE.getMessage());
        }

        return courses;
    }

    @Transactional
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
            request.setCourseTitle(getCellValue(row, 0));
            request.setCategory(getCellValue(row, 1));
            request.setDescription(getCellValue(row, 2));
            request.setDuration(getCellValue(row, 3));
            request.setInstructor(getCellValue(row, 4));

            courseRequests.add(request);
        }

        workbook.close();

        Set<String> incomingCategories = new HashSet<>();
        Set<String> requestKeys = new HashSet<>();

        courseRequests.forEach(req -> {
            incomingCategories.add(req.getCategory());
            requestKeys.add(req.getCourseTitle() + "_" + req.getCategory());
        });
        List<Course> coursesInCategories = coursePrimaryService.findAllByCategoryIn(incomingCategories);

        List<Course> existingCourses = new ArrayList<>();
        List<Course> toDeleteCourses = new ArrayList<>();

        for (Course course : coursesInCategories) {
            if (requestKeys.contains(course.getCourseTitle() + "_" + course.getCategory())) {
                existingCourses.add(course);
            } else {
                toDeleteCourses.add(course);
            }
        }

        if (!toDeleteCourses.isEmpty()) {
            deleteEnrollmentsForCourses(toDeleteCourses);
            coursePrimaryService.deleteAll(toDeleteCourses);
            notifyUsersForCourseUpdate(toDeleteCourses, allUsers, CourseActionEnum.DELETED.name().toLowerCase(Locale.ROOT));
        }

        //Calling the createOrUpdateCourses method
        return createOrUpdateProcess(courseRequests, existingCourses, allUsers);
    }

    public String uploadPdfFile(MultipartFile file, Long courseId) {

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.lastIndexOf(".") == -1) {
            throw new IllegalArgumentException(ErrorMessageEnum.NO_DOC.getMessage());
        }
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toLowerCase();

        if (!"pdf".equals(fileExtension)) {
            throw new IllegalArgumentException(ErrorMessageEnum.INVALID_DOC_TYPE.getMessage());
        }

        Course course = coursePrimaryService.findByCourseId(courseId);
        if(course == null) {
            throw new IllegalArgumentException(ErrorMessageEnum.NO_COURSE.getMessage());
        }

        List<Enrollment> enrollments = enrollmentPrimaryService.findByCourseId(courseId);

        List<Long> userIds = enrollments.stream()
                .map(Enrollment::getUserId)
                .distinct()
                .toList();

        List<User> users = userPrimaryService.findByUserIds(userIds);

        String documentLink = uploadDocumentToS3(file);

        course.setSyllabus(documentLink);

        coursePrimaryService.save(course);

        notifyUsersForCourseUpdate(List.of(course), users, CourseActionEnum.UPDATED.name().toLowerCase(Locale.ROOT));

        return "PDF uploaded successfully";
    }

    private List<Course> createOrUpdateProcess(List<CreateUpdateRequest> createUpdateRequests, List<Course> existingCourses, List<User> allUsers) {

        // Mapping existing courses by title-category for quick lookup
        Map<String, Course> existingCourseMap = createCourseMap(existingCourses);

        Map<Long, User> userIdToUserMap = allUsers.stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));

        // Fetch all enrollments for the existing courses
        List<Long> courseIds = createCourseIdList(existingCourses);
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

        List<Course> newCourses = new ArrayList<>();
        // Process all courses (create/update)
        List<Course> coursesToSave = createUpdateRequests.stream()
                .map(req -> {
                    String key = req.getCourseTitle() + "_" + req.getCategory();
                    Course course = existingCourseMap.getOrDefault(key, Course.builder().syllabus(null).build());

                    if (course.getCourseId() == null || hasCourseChanged(course, req)) {
                        course.setCourseTitle(req.getCourseTitle());
                        course.setCategory(req.getCategory());
                        course.setDescription(req.getDescription());
                        course.setDuration(req.getDuration());
                        course.setInstructor(req.getInstructor());

                        Long courseId = course.getCourseId();

                        if (courseId != null) {
                            // Notify enrolled users for course updates
                            if (enrolledcourseIdToUsersMap.containsKey(courseId)) {
                                List<User> enrolledUsers = enrolledcourseIdToUsersMap.get(courseId);
                                notifyUsersForCourseUpdate(List.of(course), enrolledUsers, CourseActionEnum.UPDATED.name().toLowerCase(Locale.ROOT));
                            }
                        } else {
                            // Notify all users for new courses
                            newCourses.add(course);
                        }
                        return course; // Add course to the saving list
                    } else {
                        return null; // Skip course if no changes are needed
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!newCourses.isEmpty()) {
            notifyUsersForCourseUpdate(newCourses, allUsers, CourseActionEnum.ADDED.name().toLowerCase(Locale.ROOT));
        }
        // Save all courses in a single DB call
        return coursePrimaryService.saveAll(coursesToSave);
    }

    private String uploadDocumentToS3(MultipartFile file) {
        return s3Service.uploadDocument(file);//please check the credentials mentioned in application properties
        //return "aiwguyg.pdf";
    }

    private String getCellValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        return cell != null ? cell.getStringCellValue() : null;
    }

    private boolean hasCourseChanged(Course existingCourse, CreateUpdateRequest newCourseRequest) {
        return !Objects.equals(existingCourse.getCourseTitle(), newCourseRequest.getCourseTitle()) ||
                !Objects.equals(existingCourse.getCategory(), newCourseRequest.getCategory()) ||
                !Objects.equals(existingCourse.getDescription(), newCourseRequest.getDescription()) ||
                !Objects.equals(existingCourse.getDuration(), newCourseRequest.getDuration()) ||
                !Objects.equals(existingCourse.getInstructor(), newCourseRequest.getInstructor());
    }

    private List<Long> createCourseIdList(List<Course> courses) {
        return courses.stream()
                .map(Course::getCourseId)
                .collect(Collectors.toList());
    }

    private void deleteEnrollmentsForCourses(List<Course> courses) {
        List<Long> courseIds = createCourseIdList(courses);

        if(!courseIds.isEmpty()) {
            enrollmentPrimaryService.deleteByCourseIdIn(courseIds);
        }
    }

    private void notifyUsersForCourseUpdate(List<Course> courses, List<User> users, String action) {

        String subject = "Course " + action + " Notification";
        StringBuilder textBuilder = new StringBuilder("The following courses have been " + action + ":\n");

        for (Course course : courses) {
            textBuilder.append("- ")
                    .append(course.getCourseTitle())
                    .append(" (")
                    .append(course.getCategory())
                    .append(")\n");
        }

        String text = textBuilder.toString();
        List<String> emailAddresses = users.stream()
                .filter(Objects::nonNull)
                .map(User::getEmail)
                .toList();

        // Convert list to array and send email to all users at once
        emailService.sendEmail(emailAddresses.toArray(new String[0]), subject, text);
    }
}
