package com.javis.ComprehensiveLearning.security;


import com.javis.ComprehensiveLearning.constants.ErrorMessageEnum;
import com.javis.ComprehensiveLearning.model.User;
import com.javis.ComprehensiveLearning.primaryService.UserPrimaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService {

    @Autowired
    private UserPrimaryService userPrimaryService;

    public UserDetails loadUserByUserId(String userId) throws UsernameNotFoundException {
        User user = userPrimaryService.findByUserId(Long.parseLong(userId));
        if (user == null) {
            throw new UsernameNotFoundException(ErrorMessageEnum.NO_USERID+ userId);
        }
        return new UserPrincipal(user);
    }
}
