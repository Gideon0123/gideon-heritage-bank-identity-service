package com.gideonheritagebank.IDENTITY_SERVICE.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Gideon Heritage Bank - Identity Service API",
                version = "1.0.0",
                description = """
                        REST API documentation for the Gideon Heritage Bank Identity Service.

                        This service manages customer and staff identities, authentication,
                        authorization, identity verification, staff management, employment,
                        branch management, departments, addresses, and related identity
                        operations.

                        Authentication is performed using JWT access tokens.
                        Protected endpoints require a valid Bearer token.
                        """
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Enter your JWT access token."
)
public class OpenApiConfig {
}
