package com.duoc.batchservice.dto;

public record ResumenBatchResponse(
        long transaccionesProcesadas,
        long transaccionesAnomalas,
        long interesesCalculados,
        long movimientosAnuales,
        long estadosCuentaGenerados,
        long registrosOmitidos,
        long ejecucionesCompletadas
) {
}
