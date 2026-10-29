package com.gideonheritagebank.IDENTITY_SERVICE.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        JwtAuthenticationConverter jwtAuthenticationConverter =
                jwtAuthenticationConverter();

        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/auth/register",
                                "/api/v1/identity/auth/register",
                                "/api/v1/identity/register"
                        )
                        .csrfTokenRepository(
                                CookieCsrfTokenRepository.withHttpOnlyFalse()
                        )
                        .csrfTokenRequestHandler(
                                new CsrfTokenRequestAttributeHandler()
                        )
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
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
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter
                                )
                        )
                );

        return http.build();
    }

    /**
     * Maps custom JWT roles such as CUSTOMER and ADMIN to
     * Spring Security authorities ROLE_CUSTOMER and ROLE_ADMIN.
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter scopeConverter =
                new JwtGrantedAuthoritiesConverter();

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {

            Collection<GrantedAuthority> authorities =
                    new ArrayList<>(scopeConverter.convert(jwt));

            Object rolesClaim = jwt.getClaims().get("roles");

            if (rolesClaim instanceof Collection<?> roles) {
                for (Object role : roles) {
                    if (role instanceof String roleName
                            && !roleName.isBlank()) {

                        authorities.add(
                                new SimpleGrantedAuthority(
                                        "ROLE_" + roleName
                                )
                        );
                    }
                }
            }

            return authorities;
        });

        return converter;
    }
}
