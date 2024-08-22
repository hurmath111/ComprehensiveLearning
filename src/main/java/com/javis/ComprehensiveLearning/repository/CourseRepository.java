package com.javis.ComprehensiveLearning.repository;

import com.javis.ComprehensiveLearning.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query(value = "SELECT c FROM Course c WHERE (c.title, c.category) IN :titleCategoryPairs", nativeQuery = true)
    List<Course> findByTitleAndCategoryIn(@Param("titleCategoryPairs") List<Object[]> titleCategoryPairs);


    List<Course> findByCourseIdIn(List<Long> courseIds);
}