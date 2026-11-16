package com.loancalculator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void publishesLoanContractWithRequestExamplesAndProblemResponses() throws Exception {
        String operation = "$.paths['/api/v1/loans/amortization-schedule'].post";
        String request = "$.components.schemas.AmortizationScheduleRequest";

        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.info.title").value("Loan Calculator API"))
                .andExpect(jsonPath(operation + ".requestBody.required").value(true))
                .andExpect(jsonPath(operation + ".requestBody.content['application/json'].schema['$ref']")
                        .value("#/components/schemas/AmortizationScheduleRequest"))
                .andExpect(jsonPath(request + ".required", containsInAnyOrder(
                        "startDate", "endDate", "firstPaymentDate", "principal", "annualInterestRate")))
                .andExpect(jsonPath(request + ".properties.*", hasSize(5)))
                .andExpect(jsonPath(request + ".properties.startDate.format").value("date"))
                .andExpect(jsonPath(request + ".properties.startDate.example").value("2024-01-01"))
                .andExpect(jsonPath(request + ".properties.annualInterestRate.description", containsString("percentage")))
                .andExpect(jsonPath(operation + ".responses['200'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/AmortizationScheduleResponse"))
                .andExpect(jsonPath(operation + ".responses['400'].content['application/problem+json'].schema['$ref']")
                        .value("#/components/schemas/ProblemDetail"))
                .andExpect(jsonPath(operation + ".responses['422'].content['application/problem+json'].schema['$ref']")
                        .value("#/components/schemas/ProblemDetail"))
                .andExpect(jsonPath(operation + ".responses['500'].content['application/problem+json'].schema['$ref']")
                        .value("#/components/schemas/ProblemDetail"));
    }

    @Test
    void servesSwaggerUiAndItsLocalAssets() throws Exception {
        mvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("swagger-ui-bundle.js")));
        mvc.perform(get("/swagger-ui/swagger-ui-bundle.js"))
                .andExpect(status().isOk());
    }

    @Test
    void configuresSwaggerUiToLoadTheApiDefinition() throws Exception {
        mvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/v3/api-docs"));
    }
}
