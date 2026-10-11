package com.duoc.clientesservice.service;

import com.duoc.clientesservice.dto.ClienteRequest;
import com.duoc.clientesservice.exception.ClienteNoEncontradoException;
import com.duoc.clientesservice.repository.ClienteRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClienteServiceTest {

    private final ClienteRepository repository = mock(ClienteRepository.class);
    private final ClienteService service = new ClienteService(repository);

    @Test
    void informaCuandoClienteNoExiste() {
        when(repository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ClienteNoEncontradoException.class, () -> service.buscar(999L));
    }

    @Test
    void evitaRutDuplicado() {
        when(repository.existsByRutIgnoreCase("12345678-5")).thenReturn(true);
        ClienteRequest request = new ClienteRequest(
                "12345678-5", "Cliente Prueba", "cliente@banco.cl", "+56911111111", "PERSONA");
        assertThrows(IllegalArgumentException.class, () -> service.crear(request));
    }
}
