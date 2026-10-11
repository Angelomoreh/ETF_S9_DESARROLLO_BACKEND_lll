package com.duoc.batchservice.processor;

import com.duoc.batchservice.model.LegacyTransaccion;
import com.duoc.batchservice.model.TransaccionProcesada;
import com.duoc.batchservice.util.FechaLegacyParser;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.util.Locale;

public class TransaccionProcessor implements ItemProcessor<LegacyTransaccion, TransaccionProcesada> {

    @Override
    public TransaccionProcesada process(LegacyTransaccion item) {
        Long id = parseLong(item.id(), "id");
        BigDecimal monto = parseMonto(item.monto());
        String tipo = normalizarTipo(item.tipo());
        boolean anomalia = monto.compareTo(new BigDecimal("2500")) >= 0;
        return new TransaccionProcesada(id, FechaLegacyParser.parsear(item.fecha()), monto, tipo, anomalia);
    }

    private Long parseLong(String valor, String campo) {
        try {
            return Long.valueOf(valor);
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("Valor invalido para " + campo + ": " + valor, error);
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

    private String normalizarTipo(String valor) {
        String tipo = valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
        if (!tipo.equals("CREDITO") && !tipo.equals("DEBITO")) {
            throw new IllegalArgumentException("Tipo de transaccion invalido: " + valor);
        }
        return tipo;
    }
}
