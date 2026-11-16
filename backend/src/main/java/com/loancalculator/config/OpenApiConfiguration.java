package com.loancalculator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean
    public OpenAPI loanOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Loan Calculator API")
                .version("v1")
                .description("Calculate loan amortization schedules and interest. Annual interest rates are percentages."));
    }
}
