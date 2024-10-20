package com.javis.ComprehensiveLearning.service;

import com.javis.ComprehensiveLearning.dto.CourseTitleAndCategory;
import com.javis.ComprehensiveLearning.model.Course;
import com.javis.ComprehensiveLearning.primaryService.CoursePrimaryService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BaseCourseService {

    @Autowired
    protected CoursePrimaryService coursePrimaryService;

    protected <T extends CourseTitleAndCategory> List<Course> fetchAndFilterCourses(List<T> courseRequests) {
        Set<String> courseTitlesSet = new HashSet<>();
        Set<String> categoriesSet = new HashSet<>();
        Set<String> validPairs = new HashSet<>();

        for (CourseTitleAndCategory req : courseRequests) {
            courseTitlesSet.add(req.getCourseTitle());
            categoriesSet.add(req.getCategory());
            validPairs.add(req.getCourseTitle() + "_" + req.getCategory());
        }

        List<String> courseTitles = new ArrayList<>(courseTitlesSet);
        List<String> categories = new ArrayList<>(categoriesSet);

        List<Course> allMatchingCourses = coursePrimaryService.findByCourseTitleInAndCategoryIn(courseTitles, categories);

        return allMatchingCourses.stream()
                .filter(course -> validPairs.contains(course.getCourseTitle() + "_" + course.getCategory()))
                .collect(Collectors.toList());
    }

    protected Map<String, Course> createCourseMap(List<Course> courses) {
        return courses.stream()
                .collect(Collectors.toMap(
                        course -> course.getCourseTitle() + "_" + course.getCategory(),
                        Function.identity()//test adding a comment
                ));
    }
}
