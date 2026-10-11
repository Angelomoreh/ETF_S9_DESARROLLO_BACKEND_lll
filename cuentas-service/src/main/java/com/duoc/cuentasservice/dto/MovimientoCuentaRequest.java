package com.duoc.cuentasservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MovimientoCuentaRequest(
        @NotBlank @Pattern(regexp = "CREDITO|DEBITO") String tipo,
        @NotNull @DecimalMin(value = "1.00") BigDecimal monto,
        @NotBlank @Size(max = 120) String referencia
) {
}
