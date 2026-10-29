package com.gideonheritagebank.IDENTITY_SERVICE.config;

import jakarta.servlet.http.Cookie;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import java.util.ArrayList;
import java.util.Collection;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public BearerTokenResolver bearerTokenResolver(
            @Value("${identity.security.cookies.access-name:ACCESS_TOKEN}")
            String accessCookieName) {

        DefaultBearerTokenResolver headerTokenResolver =
                new DefaultBearerTokenResolver();

        return request -> {
            String authorizationHeader =
                    request.getHeader(HttpHeaders.AUTHORIZATION);

            // Preserve standard Authorization: Bearer authentication.
            // When supplied, it takes priority over cookie authentication.
            if (authorizationHeader != null
                    && authorizationHeader.regionMatches(
                    true, 0, "Bearer", 0, 6
            )) {

                return headerTokenResolver.resolve(request);
            }

            // Otherwise, authenticate using the access-token cookie.
            Cookie[] cookies = request.getCookies();

            if (cookies == null) {
                return null;
            }

            for (Cookie cookie : cookies) {
                if (accessCookieName.equals(cookie.getName())
                        && cookie.getValue() != null
                        && !cookie.getValue().isBlank()) {

                    return cookie.getValue();
                }
            }

            return null;
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            BearerTokenResolver bearerTokenResolver,
            SecurityApiErrorHandler securityApiErrorHandler
    ) throws Exception {

        JwtAuthenticationConverter jwtAuthenticationConverter = jwtAuthenticationConverter();

        http
                .csrf(csrf -> csrf

                        .ignoringRequestMatchers(
                                "/auth/register",
                                "/api/v1/identity/auth/register",
                                "/api/v1/identity/register",

                                "/auth/login",
                                "/api/v1/identity/auth/login",

                                "/auth/verify-email",
                                "/api/v1/identity/auth/verify-email",
                                "/auth/forgot-password",
                                "/api/v1/identity/auth/forgot-password",
                                "/auth/reset-password",
                                "/api/v1/identity/auth/reset-password",
                                "/auth/refresh-token",
                                "/api/v1/identity/auth/refresh-token"
                        )

                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(authorize -> authorize

                        .requestMatchers(
                                HttpMethod.GET,
                                "/.well-known/jwks.json",
                                "/actuator/health",
                                "/auth/csrf",
                                "/api/v1/identity/auth/csrf",
                                "/api/v1/identity/csrf"
                        ).permitAll()

                        .requestMatchers(
                                "/auth/login",
                                "/auth/register",
                                "/auth/verify-email",
                                "/auth/forgot-password",
                                "/auth/reset-password",
                                "/auth/refresh-token",
                                "/auth/logout",

                                "/api/v1/identity/auth/login",
                                "/api/v1/identity/auth/register",
                                "/api/v1/identity/auth/verify-email",
                                "/api/v1/identity/auth/forgot-password",
                                "/api/v1/identity/auth/reset-password",
                                "/api/v1/identity/auth/refresh-token",
                                "/api/v1/identity/auth/logout",

                                "/api/v1/identity/register",
                                "/api/v1/identity/verify-email",
                                "/api/v1/identity/forgot-password",
                                "/api/v1/identity/reset-password"
                        ).permitAll()

                        .anyRequest().authenticated()
                )

                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(bearerTokenResolver)
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter
                                )
                        )
                )

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(securityApiErrorHandler)
                        .accessDeniedHandler(securityApiErrorHandler)
                );

        return http.build();
    }

    /**
     * Maps JWT roles such as ADMIN and CUSTOMER to
     * Spring Security authorities ROLE_ADMIN and ROLE_CUSTOMER.
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {

            Collection<GrantedAuthority> authorities = new ArrayList<>(scopeConverter.convert(jwt));

            Object rolesClaim = jwt.getClaims().get("roles");

            if (rolesClaim instanceof Collection<?> roles) {
                for (Object role : roles) {
                    if (role instanceof String roleName && !roleName.isBlank()) {

                        String authority = roleName.startsWith("ROLE_")
                                ? roleName : "ROLE_" + roleName;

                        authorities.add(new SimpleGrantedAuthority(authority));
                    }
                }
            }

            return authorities;
        });

        return converter;
    }
}