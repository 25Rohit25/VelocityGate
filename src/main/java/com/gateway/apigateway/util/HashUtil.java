package com.gateway.apigateway.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.codec.digest.DigestUtils;

@UtilityClass
public class HashUtil {

    public static String hashApiKey(String apiKey) {
        return DigestUtils.sha256Hex(apiKey);
    }
}
