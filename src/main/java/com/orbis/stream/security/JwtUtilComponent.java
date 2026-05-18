package com.orbis.stream.security;

import com.orbis.stream.configuration.SecurityConfigurations;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;

@Component
public class JwtUtilComponent {


    private final SecurityConfigurations securityConfigurations;


    public JwtUtilComponent(SecurityConfigurations securityConfigurations) {
        this.securityConfigurations = securityConfigurations;
    }

    protected String generateToken(String subject) {

        String token;
        String secretKey = getValueFromEnv();

        SecretKey secretKeyHmac = Keys.hmacShaKeyFor(
                secretKey.getBytes(StandardCharsets.UTF_8)
        );

        long timestampMillis = System.currentTimeMillis();
        long expirationDateMillis = getExpiration(timestampMillis);

        token = Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(expirationDateMillis))
                .signWith(secretKeyHmac, SignatureAlgorithm.HS256)
                .compact();

        return token;
    }

    private String getValueFromEnv() {
        return securityConfigurations.retrievesConfigurations();
    }

    private long getExpiration(long timestampMillis){

        return Instant.ofEpochMilli(timestampMillis)
                .atZone(ZoneId.systemDefault())
                .plusMonths(1)
                .toInstant()
                .toEpochMilli();
    }
}
