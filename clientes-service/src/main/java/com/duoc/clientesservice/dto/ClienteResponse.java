package com.duoc.clientesservice.dto;

import java.time.OffsetDateTime;

public record ClienteResponse(
        Long id,
        String rut,
        String nombre,
        String email,
        String telefono,
        String segmento,
        Boolean activo,
        OffsetDateTime fechaActualizacion
) {
}
