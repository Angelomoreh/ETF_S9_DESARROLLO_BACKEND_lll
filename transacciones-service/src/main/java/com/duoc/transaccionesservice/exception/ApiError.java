package com.duoc.transaccionesservice.exception;

import java.time.OffsetDateTime;

public record ApiError(OffsetDateTime fecha, int estado, String error, String mensaje, String ruta) {
}
