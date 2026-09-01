package com.pelia.mtgfinder.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String UNAUTHORIZED_RESPONSE = """
            {"status":401,"error":"Unauthorized","message":"Invalid or missing API key"}
            """;

    private final byte[] expectedApiKey;
    private final String apiKeyHeader;

    public ApiKeyAuthenticationFilter(
            @Value("${com.pelia.mtgfinder.api-key}") String apiKey,
            @Value("${com.pelia.mtgfinder.api-key-header:X-API-Key}") String apiKeyHeader) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("The API key must not be blank");
        }
        if (apiKeyHeader == null || apiKeyHeader.isBlank()) {
            throw new IllegalArgumentException("The API key header must not be blank");
        }

        this.expectedApiKey = apiKey.getBytes(StandardCharsets.UTF_8);
        this.apiKeyHeader = apiKeyHeader;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String providedApiKey = request.getHeader(apiKeyHeader);

        if (!isValid(providedApiKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(UNAUTHORIZED_RESPONSE);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isValid(String providedApiKey) {
        return providedApiKey != null
                && MessageDigest.isEqual(
                        expectedApiKey,
                        providedApiKey.getBytes(StandardCharsets.UTF_8));
    }
}
