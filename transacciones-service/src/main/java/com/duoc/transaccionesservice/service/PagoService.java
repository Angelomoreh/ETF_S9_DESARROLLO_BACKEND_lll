package com.duoc.transaccionesservice.service;

import com.duoc.transaccionesservice.client.CuentaOperacionClient;
import com.duoc.transaccionesservice.dto.DepositoRequest;
import com.duoc.transaccionesservice.dto.MovimientoCuentaRequest;
import com.duoc.transaccionesservice.dto.PagoRequest;
import com.duoc.transaccionesservice.dto.PagoResponse;
import com.duoc.transaccionesservice.dto.TransferenciaRequest;
import com.duoc.transaccionesservice.event.PagoEvent;
import com.duoc.transaccionesservice.event.PagoEventPublisher;
import com.duoc.transaccionesservice.exception.OperacionPagoException;
import com.duoc.transaccionesservice.model.Pago;
import com.duoc.transaccionesservice.model.Transaccion;
import com.duoc.transaccionesservice.repository.PagoRepository;
import com.duoc.transaccionesservice.repository.TransaccionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PagoService {

    private final CuentaOperacionClient cuentas;
    private final PagoRepository pagos;
    private final TransaccionRepository transacciones;
    private final PagoEventPublisher eventos;

    public PagoService(
            CuentaOperacionClient cuentas,
            PagoRepository pagos,
            TransaccionRepository transacciones,
            PagoEventPublisher eventos
    ) {
        this.cuentas = cuentas;
        this.pagos = pagos;
        this.transacciones = transacciones;
        this.eventos = eventos;
    }

    @Transactional
    public PagoResponse pagar(PagoRequest request) {
        String operacionId = UUID.randomUUID().toString();
        cuentas.aplicar(request.cuentaOrigenId(), new MovimientoCuentaRequest(
                "DEBITO", request.monto(), operacionId));
        Pago pago = guardar(operacionId, "PAGO", request.cuentaOrigenId(), null,
                request.monto(), request.descripcion(), "APROBADO");
        registrarTransaccion(request.cuentaOrigenId(), "DEBITO", request.monto(), request.descripcion());
        publicar(pago);
        return aRespuesta(pago);
    }

    @Transactional
    public PagoResponse depositar(DepositoRequest request) {
        String operacionId = UUID.randomUUID().toString();
        cuentas.aplicar(request.cuentaDestinoId(), new MovimientoCuentaRequest(
                "CREDITO", request.monto(), operacionId));
        Pago pago = guardar(operacionId, "DEPOSITO", null, request.cuentaDestinoId(),
                request.monto(), request.descripcion(), "APROBADO");
        registrarTransaccion(request.cuentaDestinoId(), "CREDITO", request.monto(), request.descripcion());
        publicar(pago);
        return aRespuesta(pago);
    }

    @Transactional(noRollbackFor = OperacionPagoException.class)
    public PagoResponse transferir(TransferenciaRequest request) {
        if (request.cuentaOrigenId().equals(request.cuentaDestinoId())) {
            throw new OperacionPagoException("La cuenta de origen y destino deben ser distintas");
        }

        String operacionId = UUID.randomUUID().toString();
        cuentas.aplicar(request.cuentaOrigenId(), new MovimientoCuentaRequest(
                "DEBITO", request.monto(), operacionId));
        try {
            cuentas.aplicar(request.cuentaDestinoId(), new MovimientoCuentaRequest(
                    "CREDITO", request.monto(), operacionId));
        } catch (RuntimeException error) {
            cuentas.aplicar(request.cuentaOrigenId(), new MovimientoCuentaRequest(
                    "CREDITO", request.monto(), operacionId + "-COMPENSACION"));
            guardar(operacionId, "TRANSFERENCIA", request.cuentaOrigenId(), request.cuentaDestinoId(),
                    request.monto(), request.descripcion(), "COMPENSADO");
            throw new OperacionPagoException("La transferencia fallo y el debito fue compensado");
        }

        Pago pago = guardar(operacionId, "TRANSFERENCIA", request.cuentaOrigenId(),
                request.cuentaDestinoId(), request.monto(), request.descripcion(), "APROBADO");
        registrarTransaccion(request.cuentaOrigenId(), "DEBITO", request.monto(), request.descripcion());
        registrarTransaccion(request.cuentaDestinoId(), "CREDITO", request.monto(), request.descripcion());
        publicar(pago);
        return aRespuesta(pago);
    }

    @Transactional(readOnly = true)
    public List<PagoResponse> listarRecientes(int limite) {
        int limiteSeguro = Math.max(1, Math.min(limite, 50));
        return pagos.findAllByOrderByFechaDesc(PageRequest.of(0, limiteSeguro)).stream()
                .map(this::aRespuesta)
                .toList();
    }

    private Pago guardar(
            String operacionId,
            String tipo,
            Long origen,
            Long destino,
            java.math.BigDecimal monto,
            String descripcion,
            String estado
    ) {
        return pagos.save(new Pago(
                operacionId, tipo, origen, destino, monto, descripcion, estado, OffsetDateTime.now()));
    }

    private void registrarTransaccion(
            Long cuentaId,
            String tipo,
            java.math.BigDecimal monto,
            String descripcion
    ) {
        transacciones.save(new Transaccion(
                cuentaId, LocalDate.now(), tipo, monto, descripcion, "PAGOS", false));
    }

    private void publicar(Pago pago) {
        eventos.publicar(new PagoEvent(
                pago.getOperacionId(), pago.getTipo(), pago.getCuentaOrigenId(),
                pago.getCuentaDestinoId(), pago.getMonto(), pago.getEstado(), pago.getFecha()));
    }

    private PagoResponse aRespuesta(Pago pago) {
        return new PagoResponse(
                pago.getOperacionId(), pago.getTipo(), pago.getCuentaOrigenId(),
                pago.getCuentaDestinoId(), pago.getMonto(), pago.getEstado(),
                pago.getDescripcion(), pago.getFecha());
    }
}
