package com.duoc.batchservice.processor;

import com.duoc.batchservice.model.LegacyInteres;
import com.duoc.batchservice.model.LegacyMovimiento;
import com.duoc.batchservice.model.LegacyTransaccion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcesadoresBatchTest {

    @Test
    void procesaTransaccionValidaYDetectaAnomalia() throws Exception {
        var resultado = new TransaccionProcessor().process(
                new LegacyTransaccion("1", "2024-06-30", "5000", "credito"));
        assertEquals("CREDITO", resultado.tipo());
        assertTrue(resultado.anomalia());
    }

    @Test
    void omiteTransaccionConTipoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new TransaccionProcessor().process(
                new LegacyTransaccion("2", "2024-06-30", "500", "invalid")));
    }

    @Test
    void calculaInteresDeCuentaAhorro() throws Exception {
        var resultado = new InteresProcessor().process(
                new LegacyInteres("101", "Diana Prince", "10000", "35", "ahorro"));
        assertEquals("50.00", resultado.interesCalculado().toPlainString());
    }

    @Test
    void normalizaMovimientoAnual() throws Exception {
        var resultado = new MovimientoAnualProcessor().process(
                new LegacyMovimiento("101", "24/03/2024", "deposito", "1000", "Ingreso"));
        assertEquals("DEPOSITO", resultado.transaccion());
        assertEquals(64, resultado.fuenteHash().length());
    }
}
