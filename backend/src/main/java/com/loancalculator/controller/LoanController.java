package com.loancalculator.controller;

import com.loancalculator.dto.AmortizationScheduleRequest;
import com.loancalculator.dto.AmortizationScheduleResponse;
import com.loancalculator.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/loans")
public class LoanController {
    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping(value = "/amortization-schedule", consumes = "application/json", produces = "application/json")
    public AmortizationScheduleResponse calculate(@Valid @RequestBody AmortizationScheduleRequest request) {
        return loanService.calculate(request);
    }
}
