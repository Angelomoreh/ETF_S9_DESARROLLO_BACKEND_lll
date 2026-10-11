package com.duoc.batchservice.model;

public record LegacyMovimiento(
        String cuentaId,
        String fecha,
        String transaccion,
        String monto,
        String descripcion
) {
}
