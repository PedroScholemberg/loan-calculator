package com.loancalculator.config;

import com.loancalculator.domain.LoanCalculator;
import com.loancalculator.domain.PaymentScheduleGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class LoanApiConfiguration {
    @Bean
    public PaymentScheduleGenerator paymentScheduleGenerator() {
        return new PaymentScheduleGenerator();
    }

    @Bean
    public LoanCalculator loanCalculator() {
        return new LoanCalculator();
    }

    @Bean
    public WebMvcConfigurer loanCorsConfigurer(
            @Value("${loan.api.allowed-origins}") String[] allowedOrigins) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/v1/loans/amortization-schedule")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("POST", "OPTIONS")
                        .allowedHeaders("Content-Type", "Accept");
            }
        };
    }
}
