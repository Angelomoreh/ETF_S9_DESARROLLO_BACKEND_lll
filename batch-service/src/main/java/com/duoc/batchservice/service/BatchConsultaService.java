package com.duoc.batchservice.service;

import com.duoc.batchservice.dto.ResumenBatchResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class BatchConsultaService {

    private final JdbcTemplate jdbcTemplate;

    public BatchConsultaService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ResumenBatchResponse resumen() {
        return new ResumenBatchResponse(
                contar("batch_transacciones_diarias"),
                valor("SELECT COUNT(*) FROM batch_transacciones_diarias WHERE anomalia = TRUE"),
                contar("batch_intereses_mensuales"),
                contar("batch_movimientos_anuales"),
                contar("batch_estados_cuenta_anuales"),
                contar("batch_errores"),
                valor("SELECT COUNT(*) FROM BATCH_JOB_EXECUTION WHERE STATUS = 'COMPLETED'")
        );
    }

    public List<Map<String, Object>> ejecuciones() {
        return jdbcTemplate.queryForList("""
                SELECT ji.JOB_NAME AS job,
                       je.STATUS AS estado,
                       je.START_TIME AS inicio,
                       je.END_TIME AS termino,
                       je.EXIT_CODE AS codigo_salida
                FROM BATCH_JOB_EXECUTION je
                JOIN BATCH_JOB_INSTANCE ji ON ji.JOB_INSTANCE_ID = je.JOB_INSTANCE_ID
                ORDER BY je.JOB_EXECUTION_ID DESC
                LIMIT 15
                """);
    }

    private long contar(String tabla) {
        return valor("SELECT COUNT(*) FROM " + tabla);
    }

    private long valor(String sql) {
        Long valor = jdbcTemplate.queryForObject(sql, Long.class);
        return valor == null ? 0 : valor;
    }
}
