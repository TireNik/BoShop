package com.kika.api_gateway.filter;

import com.kika.api_gateway.util.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {

        String path = request.getRequestURI();
        log.debug("Processing request to path: {}", path);

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No token found, proceeding without authentication");
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtTokenProvider.validateToken(token)) {
            log.debug("Invalid token, proceeding without authentication");
            chain.doFilter(request, response);
            return;
        }

        Claims claims = jwtTokenProvider.extractAllClaims(token);
        String email = claims.getSubject();
        String role = claims.get("role", String.class);
        Long userId = claims.get("userId", Long.class);

        log.info("✅ Valid token for user: {} with role: {}", email, role);

        HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if ("X-User-Email".equals(name)) {
                    return email;
                }
                if ("X-User-Role".equals(name)) {
                    return role;
                }
                if ("X-User-Id".equals(name) && userId != null) {
                    return userId.toString();
                }
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                List<String> names = Collections.list(super.getHeaderNames());
                names.add("X-User-Email");
                names.add("X-User-Role");
                if (userId != null) {
                    names.add("X-User-Id");
                }
                return Collections.enumeration(names);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if ("X-User-Email".equals(name)) {
                    return Collections.enumeration(Collections.singletonList(email));
                }
                if ("X-User-Role".equals(name)) {
                    return Collections.enumeration(Collections.singletonList(role));
                }
                if ("X-User-Id".equals(name) && userId != null) {
                    return Collections.enumeration(Collections.singletonList(userId.toString()));
                }
                return super.getHeaders(name);
            }
        };

        chain.doFilter(wrapper, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/public/") ||
                path.startsWith("/oauth2/") ||
                path.startsWith("/login/") ||
                path.startsWith("/actuator/") ||
                path.startsWith("/openapi.yaml") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/webjars/");
    }
}