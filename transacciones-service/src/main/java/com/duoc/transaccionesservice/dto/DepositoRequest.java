package com.duoc.transaccionesservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DepositoRequest(
        @NotNull Long cuentaDestinoId,
        @NotNull @DecimalMin(value = "1.00") BigDecimal monto,
        @NotBlank @Size(max = 200) String descripcion
) {
}
