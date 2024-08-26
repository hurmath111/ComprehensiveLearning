package com.javis.ComprehensiveLearning.controller;

import com.javis.ComprehensiveLearning.constants.ErrorMessageEnum;
import com.javis.ComprehensiveLearning.dto.LoginRequest;
import com.javis.ComprehensiveLearning.dto.LoginResponse;
import com.javis.ComprehensiveLearning.dto.UserRegistrationRequest;
import com.javis.ComprehensiveLearning.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> saveUser(@RequestBody UserRegistrationRequest userRegistrationRequest){
        try {
            userService.validateAndAddUser(userRegistrationRequest);
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch(IllegalArgumentException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse loginResponse = userService.authenticateUser(loginRequest);
            return ResponseEntity.ok(loginResponse);
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

}
