package com.duoc.batchservice.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record EjecucionBatchResponse(
        String ejecucionId,
        String estadoGeneral,
        OffsetDateTime iniciada,
        OffsetDateTime finalizada,
        boolean ejecucionParalela,
        List<ResultadoJob> jobs
) {
}
