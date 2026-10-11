package com.duoc.transaccionesservice.controller;

import com.duoc.transaccionesservice.dto.DepositoRequest;
import com.duoc.transaccionesservice.dto.PagoRequest;
import com.duoc.transaccionesservice.dto.PagoResponse;
import com.duoc.transaccionesservice.dto.TransferenciaRequest;
import com.duoc.transaccionesservice.service.PagoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService service;

    public PagoController(PagoService service) {
        this.service = service;
    }

    @GetMapping
    public List<PagoResponse> listar(@RequestParam(defaultValue = "10") int limite) {
        return service.listarRecientes(limite);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PagoResponse pagar(@Valid @RequestBody PagoRequest request) {
        return service.pagar(request);
    }

    @PostMapping("/depositos")
    @ResponseStatus(HttpStatus.CREATED)
    public PagoResponse depositar(@Valid @RequestBody DepositoRequest request) {
        return service.depositar(request);
    }

    @PostMapping("/transferencias")
    @ResponseStatus(HttpStatus.CREATED)
    public PagoResponse transferir(@Valid @RequestBody TransferenciaRequest request) {
        return service.transferir(request);
    }
}
