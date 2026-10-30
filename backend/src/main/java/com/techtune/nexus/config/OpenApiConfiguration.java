package com.techtune.nexus.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI nexusCoreOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NEXUS CORE API")
                        .description("Persistent background job processing and automation platform")
                        .version("0.0.1-SNAPSHOT")
                        .license(new License()
                                .name("MIT")
                                .url("https://github.com/TECHTUNE-I-T-SOLUTIONS/nexus-core")))
                .addSecurityItem(new SecurityRequirement().addList("X-API-Key"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("X-API-Key",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-API-Key")));
    }
}
