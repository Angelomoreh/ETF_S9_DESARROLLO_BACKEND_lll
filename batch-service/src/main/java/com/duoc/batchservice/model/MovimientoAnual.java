package com.duoc.batchservice.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoAnual(
        String fuenteHash,
        Long cuentaId,
        LocalDate fecha,
        String transaccion,
        BigDecimal monto,
        String descripcion
) {
}
