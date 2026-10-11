package com.duoc.batchservice.processor;

import com.duoc.batchservice.model.LegacyMovimiento;
import com.duoc.batchservice.model.MovimientoAnual;
import com.duoc.batchservice.util.FechaLegacyParser;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

public class MovimientoAnualProcessor implements ItemProcessor<LegacyMovimiento, MovimientoAnual> {

    @Override
    public MovimientoAnual process(LegacyMovimiento item) {
        Long cuentaId = parseCuenta(item.cuentaId());
        BigDecimal monto = parseMonto(item.monto());
        String transaccion = normalizarTransaccion(item.transaccion());
        String descripcion = item.descripcion() == null || item.descripcion().isBlank()
                ? "Movimiento legacy sin descripcion"
                : item.descripcion().trim();
        String fuenteHash = hash(String.join("|",
                item.cuentaId(), item.fecha(), item.transaccion(), item.monto(), descripcion));

        return new MovimientoAnual(
                fuenteHash,
                cuentaId,
                FechaLegacyParser.parsear(item.fecha()),
                transaccion,
                monto,
                descripcion
        );
    }

    private Long parseCuenta(String valor) {
        try {
            return Long.valueOf(valor);
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("Cuenta invalida: " + valor, error);
        }
    }

    private BigDecimal parseMonto(String valor) {
        try {
            BigDecimal monto = new BigDecimal(valor);
            if (monto.signum() <= 0) {
                throw new IllegalArgumentException("El monto debe ser mayor que cero");
            }
            return monto;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Monto invalido: " + valor, error);
        }
    }

    private String normalizarTransaccion(String valor) {
        String tipo = valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
        if (!tipo.equals("DEPOSITO") && !tipo.equals("RETIRO") && !tipo.equals("COMPRA")) {
            throw new IllegalArgumentException("Movimiento anual invalido: " + valor);
        }
        return tipo;
    }

    private String hash(String valor) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 no se encuentra disponible", error);
        }
    }
}
