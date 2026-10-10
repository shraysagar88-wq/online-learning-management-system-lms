package com.lms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
class TokenService {
    private final byte[] secret;

    TokenService(@Value("${app.auth.secret:change-this-development-secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    String issue(User user) {
        String payload = user.getId() + "." + user.getRole().name();
        return "gu-lms." + payload + "." + sign(payload);
    }

    Long userId(String token) {
        if (token == null || !token.startsWith("gu-lms.")) return null;
        String value = token.substring("gu-lms.".length());
        String[] parts = value.split("\\.", -1);
        if (parts.length != 3 || !MessageDigest.isEqual(parts[2].getBytes(StandardCharsets.UTF_8), sign(parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8))) return null;
        try { return Long.valueOf(parts[0]); } catch (NumberFormatException ex) { return null; }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) { throw new IllegalStateException("Unable to sign authentication token", ex); }
    }
}
