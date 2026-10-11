package com.duoc.transaccionesservice.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MovimientoCuentaResponse(
        Long cuentaId,
        String tipo,
        BigDecimal monto,
        BigDecimal saldoAnterior,
        BigDecimal saldoActual,
        String referencia,
        OffsetDateTime fecha
) {
}
