package in.abhiikhula.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI abhiiKhulaOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token directly or with 'Bearer ' prefix.")))
                .info(new Info()
                        .title("AbhiiKhula API Documentation")
                        .description("Hyperlocal Live Local Availability & Virtual Queue Platform (BRD: AKH-BRD-001 v1.1)")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("AbhiiKhula Team")
                                .url("https://abhiikhula.in")
                                .email("founder@abhiikhula.in"))
                        .license(new License().name("Internal - Confidential")));
    }
}

