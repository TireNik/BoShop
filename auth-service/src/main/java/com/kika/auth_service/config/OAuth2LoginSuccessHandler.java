package com.kika.auth_service.config;

import com.kika.auth_service.dto.AuthUserDto;
import com.kika.auth_service.security.JwtTokenProvider;
import com.kika.auth_service.service.AuthService;
import com.kika.avro.UserCreatedEvent;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final KafkaTemplate<String, UserCreatedEvent> kafkaTemplate;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();

        String email = extractEmail(oAuth2User, registrationId);
        String name = extractName(oAuth2User, registrationId);

        if (!authService.existsByEmail(email)) {
            AuthUserDto dto = new AuthUserDto();
            dto.setEmail(email);
            dto.setName(name);
            dto.setPassword(UUID.randomUUID().toString());
            authService.register(dto);

            UserCreatedEvent event = UserCreatedEvent.newBuilder()
                    .setUserId(authService.findByEmail(email).getId())
                    .setEmail(email)
                    .setName(name)
                    .build();
            kafkaTemplate.send("user-created", String.valueOf(event.getUserId()), event);
        }

        var user = authService.findByEmail(email);
        String token = jwtTokenProvider.createAccessToken(
                user.getEmail(), user.getId(), user.getRole().name()
        );

        String redirectUrl = "http://localhost:3000/oauth/success?token=" + token;
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private String extractEmail(OAuth2User user, String provider) {
        switch (provider) {
            case "google":
                return (String) user.getAttributes().get("email");
            case "github":
                Object email = user.getAttributes().get("email");
                if (email == null)
                    email = user.getAttributes().get("login") + "@github.com";
                return email.toString();
            default:
                throw new IllegalArgumentException("Unsupported provider: " + provider);
        }
    }

    private String extractName(OAuth2User user, String provider) {
        switch (provider) {
            case "google":
                return (String) user.getAttributes().get("name");
            case "github":
                return (String) user.getAttributes().getOrDefault("name", user.getAttributes().get("login"));
            default:
                return "Unknown";
        }
    }

}