package com.duoc.transaccionesservice.client;

import com.duoc.transaccionesservice.dto.MovimientoCuentaRequest;
import com.duoc.transaccionesservice.dto.MovimientoCuentaResponse;
import com.duoc.transaccionesservice.exception.OperacionPagoException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class CuentaOperacionClient {

    private final RestClient restClient;

    public CuentaOperacionClient(@Qualifier("cuentasRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @CircuitBreaker(name = "cuentas", fallbackMethod = "fallbackMovimiento")
    public MovimientoCuentaResponse aplicar(Long cuentaId, MovimientoCuentaRequest request) {
        try {
            return restClient.post()
                    .uri("/api/cuentas/{id}/movimientos", cuentaId)
                    .body(request)
                    .retrieve()
                    .body(MovimientoCuentaResponse.class);
        } catch (RestClientResponseException exception) {
            throw new OperacionPagoException("La cuenta rechazo el movimiento: " + exception.getStatusText());
        }
    }

    public MovimientoCuentaResponse fallbackMovimiento(
            Long cuentaId,
            MovimientoCuentaRequest request,
            Throwable error
    ) {
        throw new OperacionPagoException("Servicio de cuentas no disponible. La operacion no fue confirmada");
    }
}
