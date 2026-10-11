package com.duoc.transaccionesservice.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PagoResponse(
        String operacionId,
        String tipo,
        Long cuentaOrigenId,
        Long cuentaDestinoId,
        BigDecimal monto,
        String estado,
        String descripcion,
        OffsetDateTime fecha
) {
}
