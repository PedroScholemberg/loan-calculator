package com.loancalculator.controller;

import com.loancalculator.dto.AmortizationScheduleRequest;
import com.loancalculator.dto.AmortizationScheduleResponse;
import com.loancalculator.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/loans")
@Tag(name = "Loans", description = "Loan amortization calculations")
public class LoanController {
    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping(value = "/amortization-schedule", consumes = "application/json", produces = "application/json")
    @Operation(summary = "Calculate an amortization schedule",
            description = "Returns chronological schedule entries and payment totals. "
                    + "The first payment date must be strictly between the start and end dates.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Schedule calculated successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AmortizationScheduleResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request body or loan parameters",
                            content = @Content(mediaType = "application/problem+json",
                                    schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "422", description = "Parameters cannot produce a valid financial result",
                            content = @Content(mediaType = "application/problem+json",
                                    schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "500", description = "Unexpected calculation failure",
                            content = @Content(mediaType = "application/problem+json",
                                    schema = @Schema(implementation = ProblemDetail.class)))
            })
    public AmortizationScheduleResponse calculate(@Valid @RequestBody AmortizationScheduleRequest request) {
        return loanService.calculate(request);
    }
}
