package com.javis.ComprehensiveLearning.primaryService;

import com.javis.ComprehensiveLearning.constants.RoleEnum;
import com.javis.ComprehensiveLearning.model.User;
import com.javis.ComprehensiveLearning.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserPrimaryService {

    @Autowired
    private UserRepository userRepository;

    public void saveUser(User user) {
        userRepository.save(user);
    }

    public User findByUserName(String username) {
        return userRepository.findByUserName(username);
    }

    public User findByUserId(Long userId) {
        return userRepository.findByUserId(userId);
    }

    public List<User> findByUserIds(List<Long> userIds) {
        return userRepository.findByUserIdIn(userIds);
    }

    public List<User> findAllByRole(RoleEnum role) {
        return userRepository.findAllByRole(role);
    }

    public boolean existsByUserName(String userName) {
        return userRepository.existsByUserName(userName);
    }
}
