package com.javis.ComprehensiveLearning.constants;

import lombok.Getter;

@Getter
public enum JwtConfigEnum {
    SECRET_KEY("MySuperSecretKeyThatIsLongEnoughToBeUsedWithHS512Algorithm12345678"),
    JWT_EXPIRATION_MS(86400000L); // 1 day in milliseconds

    private final String secretKey;
    private final Long expirationMs;

    JwtConfigEnum(String secretKey) {
        this.secretKey = secretKey;
        this.expirationMs = null;
    }

    JwtConfigEnum(Long expirationMs) {
        this.secretKey = null;
        this.expirationMs = expirationMs;
    }
}
