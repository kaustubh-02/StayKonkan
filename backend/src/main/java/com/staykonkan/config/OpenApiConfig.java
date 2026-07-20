package com.staykonkan.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "StayKonkan API",
                version = "v1",
                description = "REST API for the StayKonkan travel & hotel booking platform "
                        + "(Alibag, Nagaon, Murud, Kashid and nearby Konkan destinations).",
                contact = @Contact(name = "StayKonkan Engineering", email = "engineering@staykonkan.com")
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Paste the access token returned by /api/v1/auth/login (without the 'Bearer ' prefix)."
)
public class OpenApiConfig {
}
