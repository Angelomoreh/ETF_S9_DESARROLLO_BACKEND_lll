package com.duoc.transaccionesservice.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PagoEvent(
        String operacionId,
        String tipo,
        Long cuentaOrigenId,
        Long cuentaDestinoId,
        BigDecimal monto,
        String estado,
        OffsetDateTime fecha
) {
}
