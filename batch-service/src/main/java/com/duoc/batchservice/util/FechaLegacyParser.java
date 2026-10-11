package com.duoc.batchservice.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class FechaLegacyParser {

    private static final List<DateTimeFormatter> FORMATOS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd-MM-uuuu"),
            DateTimeFormatter.ofPattern("uuuu/MM/dd"),
            DateTimeFormatter.ofPattern("dd/MM/uuuu")
    );

    private FechaLegacyParser() {
    }

    public static LocalDate parsear(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("La fecha se encuentra vacia");
        }
        for (DateTimeFormatter formato : FORMATOS) {
            try {
                return LocalDate.parse(valor.trim(), formato);
            } catch (DateTimeParseException ignored) {
                // Se prueba el siguiente formato legacy permitido.
            }
        }
        throw new IllegalArgumentException("Formato de fecha invalido: " + valor);
    }
}
