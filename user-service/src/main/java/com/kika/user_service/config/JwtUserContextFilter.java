package com.kika.user_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        log.info("🔥🔥🔥 REQUEST RECEIVED: {} {}", request.getMethod(), request.getRequestURI());

        log.info("=== ALL HEADERS ===");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            log.info("Header: {} = {}", headerName, request.getHeader(headerName));
        }

        String email = request.getHeader("X-User-Email");
        String role = request.getHeader("X-User-Role");

        log.info("X-User-Email: {}, X-User-Role: {}", email, role);

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            if (role != null) {
                String formattedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                authorities.add(new SimpleGrantedAuthority(formattedRole));
                log.info("✅ Granted Authority: {}", formattedRole);
            }

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    email, null, authorities
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.info("✅ Authentication set for: {}", email);
        } else {
            log.warn("❌ No authentication set - email: {}, auth exists: {}",
                    email, SecurityContextHolder.getContext().getAuthentication() != null);
        }

        filterChain.doFilter(request, response);
    }
}