package com.javis.ComprehensiveLearning.service;

import com.javis.ComprehensiveLearning.constants.ErrorMessageEnum;
import com.javis.ComprehensiveLearning.constants.RoleEnum;
import com.javis.ComprehensiveLearning.dto.LoginRequest;
import com.javis.ComprehensiveLearning.dto.LoginResponse;
import com.javis.ComprehensiveLearning.dto.UserRegistrationRequest;
import com.javis.ComprehensiveLearning.model.User;
import com.javis.ComprehensiveLearning.primaryService.UserPrimaryService;
import com.javis.ComprehensiveLearning.security.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UserService {

    @Autowired
    private UserPrimaryService userPrimaryService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private AuthenticationManager authenticationManager;

    public void validateAndAddUser(UserRegistrationRequest userRegistrationRequest) {

        if (!isValidEmail(userRegistrationRequest.getEmail())) {
            throw new IllegalArgumentException(ErrorMessageEnum.INVALID_EMAIL.getMessage());
        }

        if (userPrimaryService.existsByuserName(userRegistrationRequest.getUserName())) {
            throw new IllegalArgumentException(ErrorMessageEnum.USERNAME_TAKEN.getMessage());
        }

        User user = new User();
        user.setUserName(userRegistrationRequest.getUserName());
        user.setEmail(userRegistrationRequest.getEmail());
        user.setPassword(passwordEncoder.encode(userRegistrationRequest.getPassword()));
        user.setRole(RoleEnum.USER);
        userPrimaryService.saveUser(user);
    }

    public LoginResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUserName(),
                        loginRequest.getPassword()
                )
        );

        return new LoginResponse(tokenProvider.generateToken(authentication));
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(emailRegex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

}
