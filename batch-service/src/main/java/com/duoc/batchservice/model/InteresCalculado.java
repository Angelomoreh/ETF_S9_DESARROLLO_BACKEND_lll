package com.duoc.batchservice.model;

import java.math.BigDecimal;

public record InteresCalculado(
        Long cuentaId,
        String nombre,
        BigDecimal saldo,
        String tipo,
        BigDecimal tasa,
        BigDecimal interesCalculado,
        String periodo
) {
}
