package com.duoc.batchservice.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransaccionProcesada(
        Long legacyId,
        LocalDate fecha,
        BigDecimal monto,
        String tipo,
        boolean anomalia
) {
}
