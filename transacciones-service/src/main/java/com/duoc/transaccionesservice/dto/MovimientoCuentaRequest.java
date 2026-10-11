package com.duoc.transaccionesservice.dto;

import java.math.BigDecimal;

public record MovimientoCuentaRequest(String tipo, BigDecimal monto, String referencia) {
}
