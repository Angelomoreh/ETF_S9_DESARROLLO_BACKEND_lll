package com.duoc.batchservice.processor;

import com.duoc.batchservice.model.InteresCalculado;
import com.duoc.batchservice.model.LegacyInteres;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Locale;

public class InteresProcessor implements ItemProcessor<LegacyInteres, InteresCalculado> {

    @Override
    public InteresCalculado process(LegacyInteres item) {
        Long cuentaId = parseLong(item.cuentaId(), "cuenta_id");
        if (item.nombre() == null || item.nombre().isBlank() || "Unknown".equalsIgnoreCase(item.nombre())) {
            throw new IllegalArgumentException("Nombre de cliente invalido");
        }
        BigDecimal saldo = parseSaldo(item.saldo());
        int edad = parseLong(item.edad(), "edad").intValue();
        if (edad < 18 || edad > 120) {
            throw new IllegalArgumentException("Edad fuera de rango: " + edad);
        }

        String tipo = item.tipo() == null ? "" : item.tipo().trim().toUpperCase(Locale.ROOT);
        BigDecimal tasa = switch (tipo) {
            case "AHORRO" -> new BigDecimal("0.00500");
            case "PRESTAMO" -> new BigDecimal("0.01500");
            case "HIPOTECA" -> new BigDecimal("0.00900");
            default -> throw new IllegalArgumentException("Tipo de cuenta invalido: " + item.tipo());
        };

        BigDecimal interes = saldo.multiply(tasa).setScale(2, RoundingMode.HALF_UP);
        return new InteresCalculado(
                cuentaId, item.nombre().trim(), saldo, tipo, tasa, interes, YearMonth.now().toString());
    }

    private Long parseLong(String valor, String campo) {
        try {
            return Long.valueOf(valor);
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("Valor invalido para " + campo + ": " + valor, error);
        }
    }

    private BigDecimal parseSaldo(String valor) {
        try {
            BigDecimal saldo = new BigDecimal(valor);
            if (saldo.signum() < 0) {
                throw new IllegalArgumentException("El saldo no puede ser negativo");
            }
            return saldo;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Saldo invalido: " + valor, error);
        }
    }
}
