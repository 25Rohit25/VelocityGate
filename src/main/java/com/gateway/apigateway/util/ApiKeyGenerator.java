package com.gateway.apigateway.util;

import lombok.experimental.UtilityClass;

import java.security.SecureRandom;
import java.util.Base64;

@UtilityClass
public class ApiKeyGenerator {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final String PREFIX = "sk_live_";

    public static String generateApiKey() {
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        String randomString = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return PREFIX + randomString;
    }
}
