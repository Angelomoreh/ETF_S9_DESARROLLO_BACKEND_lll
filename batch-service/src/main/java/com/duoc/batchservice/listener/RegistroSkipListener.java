package com.duoc.batchservice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;
import org.springframework.jdbc.core.JdbcTemplate;

public class RegistroSkipListener<I, O> implements SkipListener<I, O> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistroSkipListener.class);

    private final String jobNombre;
    private final JdbcTemplate jdbcTemplate;

    public RegistroSkipListener(String jobNombre, JdbcTemplate jdbcTemplate) {
        this.jobNombre = jobNombre;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void onSkipInRead(Throwable error) {
        registrar("LECTURA", null, error);
    }

    @Override
    public void onSkipInProcess(I item, Throwable error) {
        registrar("PROCESAMIENTO", item, error);
    }

    @Override
    public void onSkipInWrite(O item, Throwable error) {
        registrar("ESCRITURA", item, error);
    }

    private void registrar(String etapa, Object item, Throwable error) {
        String dato = item == null ? "Sin dato disponible" : item.toString();
        String mensaje = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        jdbcTemplate.update(
                """
                INSERT INTO batch_errores (job_nombre, etapa, dato_original, mensaje_error)
                VALUES (?, ?, ?, ?)
                """,
                jobNombre,
                etapa,
                dato,
                mensaje.length() > 500 ? mensaje.substring(0, 500) : mensaje
        );
        LOGGER.warn("Registro legacy omitido en {} durante {}: {}", jobNombre, etapa, mensaje);
    }
}
