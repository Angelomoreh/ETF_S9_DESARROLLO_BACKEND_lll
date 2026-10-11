package com.duoc.batchservice.dto;

public record ResultadoJob(
        String job,
        String estado,
        long leidos,
        long escritos,
        long omitidos,
        long duracionMs,
        int intentos
) {
}
