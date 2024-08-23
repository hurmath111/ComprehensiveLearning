package com.javis.ComprehensiveLearning.repository;
import com.javis.ComprehensiveLearning.model.Course;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@SuppressWarnings("unchecked")
public class CourseRepositoryImpl implements CourseRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Course> findByTitleAndCategoryIn(List<Object[]> titleCategoryPairs) {
        if (titleCategoryPairs == null || titleCategoryPairs.isEmpty()) {
            return new ArrayList<>();
        }

        StringBuilder queryBuilder = new StringBuilder("SELECT c.Course_Id, c.Course_Title, c.Category, c.Description, c.Syllabus, c.Duration, c.Instructor FROM Course c WHERE ");

        List<String> conditions = new ArrayList<>();
        for (int i = 0; i < titleCategoryPairs.size(); i++) {
            conditions.add("(c.Course_Title = :title" + i + " AND c.Category = :category" + i + ")");
        }

        queryBuilder.append(String.join(" OR ", conditions));

        Query query = entityManager.createNativeQuery(queryBuilder.toString(), Course.class);

        for (int i = 0; i < titleCategoryPairs.size(); i++) {
            query.setParameter("title" + i, titleCategoryPairs.get(i)[0]);
            query.setParameter("category" + i, titleCategoryPairs.get(i)[1]);
        }

        return query.getResultList();
    }
}
