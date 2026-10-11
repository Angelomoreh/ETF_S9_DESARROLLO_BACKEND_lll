package com.duoc.transaccionesservice.service;

import com.duoc.transaccionesservice.client.CuentaOperacionClient;
import com.duoc.transaccionesservice.dto.MovimientoCuentaResponse;
import com.duoc.transaccionesservice.dto.PagoRequest;
import com.duoc.transaccionesservice.event.PagoEventPublisher;
import com.duoc.transaccionesservice.model.Pago;
import com.duoc.transaccionesservice.repository.PagoRepository;
import com.duoc.transaccionesservice.repository.TransaccionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PagoServiceTest {

    @Test
    void procesaPagoYPublicaEvento() {
        CuentaOperacionClient cuentas = mock(CuentaOperacionClient.class);
        PagoRepository pagos = mock(PagoRepository.class);
        TransaccionRepository transacciones = mock(TransaccionRepository.class);
        PagoEventPublisher eventos = mock(PagoEventPublisher.class);
        PagoService service = new PagoService(cuentas, pagos, transacciones, eventos);

        when(cuentas.aplicar(eq(101L), any())).thenReturn(new MovimientoCuentaResponse(
                101L, "DEBITO", new BigDecimal("100"), new BigDecimal("1000"),
                new BigDecimal("900"), "prueba", OffsetDateTime.now()));
        when(pagos.save(any(Pago.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var respuesta = service.pagar(new PagoRequest(
                101L, new BigDecimal("100"), "Pago unitario"));

        assertEquals("APROBADO", respuesta.estado());
        assertEquals("PAGO", respuesta.tipo());
        verify(transacciones).save(any());
        verify(eventos).publicar(any());
    }
}
