package com.javis.ComprehensiveLearning.repository;

import com.javis.ComprehensiveLearning.constants.RoleEnum;
import com.javis.ComprehensiveLearning.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    User findByUserName(String username);
    boolean existsByUserName(String username);
    List<User> findAllByRole(RoleEnum role);
    User findByUserId(Long UserId);
    List<User> findByUserIdIn(List<Long> userIds);
}
