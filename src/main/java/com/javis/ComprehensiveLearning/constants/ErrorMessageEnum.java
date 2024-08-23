package com.javis.ComprehensiveLearning.constants;

public enum ErrorMessageEnum {
    INVALID_CREDENTIALS("Invalid credentials"),
    NO_USERID("User not found with userId: "),
    NO_USERNAME("User not found with username: "),
    NO_COURSE("Course not found"),
    NO_AUTH("You are not authorized to update this claim"),
    NO_DOC("Invalid document: No file extension found"),
    INVALID_DOC_TYPE("Invalid document type"),
    AUTO_CLAIM_REJECT("Claim auto-rejected due to low success rate"),
    NOT_YOUR_CLAIM("This claim was not made by you"),
    NO_USERS("Users Not Found"),
    NO_ADMINS("No admins available"),
    USERNAME_TAKEN("Username is already taken"),
    INVALID_EMAIL("Invalid email format");


    private final String message;

    ErrorMessageEnum(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
