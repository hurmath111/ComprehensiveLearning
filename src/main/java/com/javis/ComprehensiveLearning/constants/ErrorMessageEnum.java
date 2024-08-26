package com.javis.ComprehensiveLearning.constants;

public enum ErrorMessageEnum {
    INVALID_CREDENTIALS("Invalid credentials"),
    NO_USERID("User not found with userId: "),
    NO_USERNAME("User not found with username: "),
    NO_COURSE("Course not found"),
    NO_DOC("Invalid document: No file extension found"),
    INVALID_DOC_TYPE("Invalid document type"),
    NO_USERS("Users Not Found"),
    USERNAME_TAKEN("Username is already taken"),
    INVALID_EMAIL("Invalid email format"),
    NO_ENROLLMENTS("You dont have any courses enrolled");


    private final String message;

    ErrorMessageEnum(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
