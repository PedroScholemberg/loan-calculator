package com.loancalculator.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.loancalculator.dto.AmortizationScheduleRequest;
import com.loancalculator.dto.AmortizationScheduleResponse;
import com.loancalculator.dto.ScheduleSummary;
import com.loancalculator.exception.GlobalExceptionHandler;
import com.loancalculator.exception.LoanCalculationException;
import com.loancalculator.service.LoanService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercises actual JSON binding, Bean Validation, controller delegation and advice, without financial calculations. */
@WebMvcTest(LoanController.class)
@Import(GlobalExceptionHandler.class)
class LoanControllerTest {
    private static final String PATH = "/api/v1/loans/amortization-schedule";
    private static final String PRIVATE_DETAIL = "secret-db-password=private-marker";
    private static final String JSON_ERROR =
            "Envie um JSON válido com datas no formato AAAA-MM-DD e valores numéricos válidos.";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private LoanService service;

    @ParameterizedTest(name = "missing {0}")
    @MethodSource("requiredFields")
    void rejectsMissingRequiredFields(String field, String message) throws Exception {
        Map<String, String> values = validValues();
        values.remove(field);
        problem(submit(json(values)), HttpStatus.BAD_REQUEST, message);
        verifyNoInteractions(service);
    }

    @ParameterizedTest(name = "null {0}")
    @MethodSource("requiredFields")
    void rejectsExplicitNulls(String field, String message) throws Exception {
        problem(submit(changed(field, "null")), HttpStatus.BAD_REQUEST, message);
        verifyNoInteractions(service);
    }

    @ParameterizedTest(name = "{0}={1}")
    @MethodSource("invalidParameters")
    void rejectsInvalidDatesAndFinancialInputs(String field, String value, String message) throws Exception {
        problem(submit(changed(field, value)), HttpStatus.BAD_REQUEST, message);
        verifyNoInteractions(service);
    }

    @ParameterizedTest(name = "unreadable request: {0}")
    @MethodSource("unreadableBodies")
    void rejectsMalformedJsonInvalidDatesAndInvalidNumericTypes(String description, String body) throws Exception {
        problem(submit(body), HttpStatus.BAD_REQUEST, JSON_ERROR);
        verifyNoInteractions(service);
    }

    @ParameterizedTest(name = "valid principal {0}, annual rate {1}")
    @CsvSource({"140000.00,7", "140000.000,0", "0.01,0.125"})
    void acceptsValidRequestsAndDelegatesUnchangedValues(String principal, String rate) throws Exception {
        Map<String, String> values = validValues();
        values.put("principal", principal);
        values.put("annualInterestRate", rate);
        // Service stub only: this test verifies HTTP behavior, not financial results.
        AmortizationScheduleResponse response = new AmortizationScheduleResponse(List.of(),
                new ScheduleSummary(120, BigDecimal.ZERO, new BigDecimal(principal), new BigDecimal(principal)));
        when(service.calculate(any(AmortizationScheduleRequest.class))).thenReturn(response);

        submit(json(values))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.schedule").isArray())
                .andExpect(jsonPath("$.schedule").isEmpty())
                .andExpect(jsonPath("$.summary.installmentCount").value(120))
                .andExpect(jsonPath("$.summary.totalInterest").value(0))
                .andExpect(jsonPath("$.summary.totalAmortization").value(new BigDecimal(principal).doubleValue()))
                .andExpect(jsonPath("$.summary.totalPayment").value(new BigDecimal(principal).doubleValue()));
        verify(service).calculate(new AmortizationScheduleRequest(LocalDate.of(2024, 1, 1),
                LocalDate.of(2034, 1, 1), LocalDate.of(2024, 2, 15),
                new BigDecimal(principal), new BigDecimal(rate)));
        verifyNoMoreInteractions(service);
    }

    @ParameterizedTest(name = "calculation failure: {0}")
    @MethodSource("calculationFailures")
    void mapsCalculationExceptionsTo422WithoutLeakingCauses(LoanCalculationException exception, String message)
            throws Exception {
        when(service.calculate(any(AmortizationScheduleRequest.class))).thenThrow(exception);
        problem(submit(json(validValues())), HttpStatus.UNPROCESSABLE_CONTENT, message);
        verify(service).calculate(any(AmortizationScheduleRequest.class));
        verifyNoMoreInteractions(service);
    }

    @ParameterizedTest(name = "unexpected failure: {0}")
    @MethodSource("unexpectedFailures")
    void mapsUnexpectedExceptionsToGeneric500WithoutLeakingDetails(RuntimeException exception) throws Exception {
        when(service.calculate(any(AmortizationScheduleRequest.class))).thenThrow(exception);
        problem(submit(json(validValues())), HttpStatus.INTERNAL_SERVER_ERROR,
                "Não foi possível processar a simulação. Tente novamente mais tarde.");
        verify(service).calculate(any(AmortizationScheduleRequest.class));
        verifyNoMoreInteractions(service);
    }

    private ResultActions submit(String body) throws Exception {
        return mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON).content(body));
    }

    private static void problem(ResultActions result, HttpStatus status, String message) throws Exception {
        String body = result.andExpect(status().is(status.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                // Spring omits the default about:blank type; RFC 9457 defines that as the implicit value.
                .andExpect(jsonPath("$.type").doesNotExist())
                .andExpect(jsonPath("$.title").value(status.getReasonPhrase()))
                .andExpect(jsonPath("$.status").value(status.value()))
                .andExpect(jsonPath("$.detail", containsString(message)))
                .andExpect(jsonPath("$.instance").value(PATH))
                .andExpect(jsonPath("$.*", hasSize(4)))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        if (status != HttpStatus.BAD_REQUEST) {
            result.andExpect(jsonPath("$.detail").value(message));
        }
        // Include secret markers in injected exceptions; neither causes nor stack traces may reach the client.
        for (String internal : List.of(PRIVATE_DETAIL, "private-marker", "com.loancalculator", "java.lang",
                "Exception", "stackTrace", "MethodArgumentNotValid", "HttpMessageNotReadable", "org.springframework")) {
            assertFalse(body.contains(internal), "HTTP " + status.value() + " leaked internal detail: " + internal);
        }
    }

    private static Map<String, String> validValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("startDate", "\"2024-01-01\"");
        values.put("endDate", "\"2034-01-01\"");
        values.put("firstPaymentDate", "\"2024-02-15\"");
        values.put("principal", "140000.00");
        values.put("annualInterestRate", "7");
        return values;
    }

    private static String json(Map<String, String> values) {
        return values.entrySet().stream().map(e -> "\"" + e.getKey() + "\":" + e.getValue())
                .collect(Collectors.joining(",", "{", "}"));
    }

    private static String changed(String field, String jsonValue) {
        Map<String, String> values = validValues();
        values.put(field, jsonValue);
        return json(values);
    }

    private static Stream<Arguments> requiredFields() {
        return Stream.of(
                Arguments.of("startDate", "Informe a data inicial."),
                Arguments.of("endDate", "Informe a data final."),
                Arguments.of("firstPaymentDate", "Informe a data do primeiro pagamento."),
                Arguments.of("principal", "Informe o valor do empréstimo."),
                Arguments.of("annualInterestRate", "Informe a taxa de juros anual."));
    }

    private static Stream<Arguments> invalidParameters() {
        return Stream.of(
                Arguments.of("endDate", "\"2023-12-31\"", "A data final deve ser posterior à data inicial."),
                Arguments.of("endDate", "\"2024-01-01\"", "A data final deve ser posterior à data inicial."),
                Arguments.of("firstPaymentDate", "\"2023-12-31\"", "O primeiro pagamento deve ser posterior à data inicial."),
                Arguments.of("firstPaymentDate", "\"2024-01-01\"", "O primeiro pagamento deve ser posterior à data inicial."),
                Arguments.of("firstPaymentDate", "\"2034-01-01\"", "O primeiro pagamento deve ser anterior à data final."),
                Arguments.of("firstPaymentDate", "\"2034-01-02\"", "O primeiro pagamento deve ser anterior à data final."),
                Arguments.of("principal", "0", "O valor do empréstimo deve ser maior que zero."),
                Arguments.of("principal", "-1", "O valor do empréstimo deve ser maior que zero."),
                Arguments.of("principal", "140000.001", "Informe o valor com no máximo 2 casas decimais."),
                Arguments.of("annualInterestRate", "-0.01", "A taxa de juros não pode ser negativa."));
    }

    private static Stream<Arguments> unreadableBodies() {
        return Stream.of(
                Arguments.of("truncated JSON", "{\"principal\":"),
                Arguments.of("unquoted key", "{principal:140000}"),
                Arguments.of("missing body", ""),
                Arguments.of("wrong root type", "[]"),
                Arguments.of("Brazilian start date", changed("startDate", "\"01/01/2024\"")),
                Arguments.of("invalid end date", changed("endDate", "\"not-a-date\"")),
                Arguments.of("impossible payment date", changed("firstPaymentDate", "\"2024-02-30\"")),
                Arguments.of("invalid month", changed("startDate", "\"2024-13-01\"")),
                Arguments.of("nonnumeric principal", changed("principal", "\"" + PRIVATE_DETAIL + "\"")),
                Arguments.of("nonnumeric rate", changed("annualInterestRate", "\"" + PRIVATE_DETAIL + "\"")));
    }

    private static Stream<Arguments> calculationFailures() {
        return Stream.of(
                Arguments.of(new LoanCalculationException(),
                        "Os parâmetros informados não permitem calcular um resultado financeiro válido."),
                Arguments.of(new LoanCalculationException(new ArithmeticException(PRIVATE_DETAIL)),
                        "Os valores informados excedem a capacidade de cálculo. Revise os parâmetros."));
    }

    private static Stream<RuntimeException> unexpectedFailures() {
        return Stream.of(new IllegalStateException(PRIVATE_DETAIL, new RuntimeException("com.loancalculator.private-marker")),
                new NullPointerException(PRIVATE_DETAIL));
    }
}
