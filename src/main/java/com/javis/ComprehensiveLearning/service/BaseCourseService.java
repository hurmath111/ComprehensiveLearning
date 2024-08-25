package com.javis.ComprehensiveLearning.service;

import com.javis.ComprehensiveLearning.dto.CourseTitleAndCategory;
import com.javis.ComprehensiveLearning.model.Course;
import com.javis.ComprehensiveLearning.primaryService.CoursePrimaryService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BaseCourseService {

    @Autowired
    protected CoursePrimaryService coursePrimaryService;

    protected <T extends CourseTitleAndCategory> List<Course> fetchAndFilterCourses(List<T> courseRequests) {
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
                .map(req -> req.getCourseTitle() + "_" + req.getCategory())
                .collect(Collectors.toSet());

        return allMatchingCourses.stream()
                .filter(course -> validPairs.contains(course.getCourseTitle() + "_" + course.getCategory()))
                .collect(Collectors.toList());
    }

    protected Map<String, Course> createCourseMap(List<Course> courses) {
        return courses.stream()
                .collect(Collectors.toMap(
                        course -> course.getCourseTitle() + "_" + course.getCategory(),
                        Function.identity()
                ));
    }
}
