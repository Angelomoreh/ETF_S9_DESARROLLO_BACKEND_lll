package com.duoc.clientesservice.service;

import com.duoc.clientesservice.dto.ClienteRequest;
import com.duoc.clientesservice.dto.ClienteResponse;
import com.duoc.clientesservice.exception.ClienteNoEncontradoException;
import com.duoc.clientesservice.model.Cliente;
import com.duoc.clientesservice.repository.ClienteRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return repository.findAll(Sort.by("id")).stream().map(this::aRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscar(Long id) {
        return aRespuesta(buscarEntidad(id));
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        if (repository.existsByRutIgnoreCase(request.rut())) {
            throw new IllegalArgumentException("Ya existe un cliente con el RUT indicado");
        }
        if (repository.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("Ya existe un cliente con el correo indicado");
        }
        Cliente cliente = new Cliente(
                request.rut().toUpperCase(), request.nombre(), request.email().toLowerCase(),
                request.telefono(), request.segmento());
        return aRespuesta(repository.save(cliente));
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest request) {
        Cliente cliente = buscarEntidad(id);
        cliente.actualizar(request.nombre(), request.email().toLowerCase(), request.telefono(), request.segmento());
        return aRespuesta(cliente);
    }

    @Transactional
    public void desactivar(Long id) {
        buscarEntidad(id).desactivar();
    }

    private Cliente buscarEntidad(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe el cliente " + id));
    }

    private ClienteResponse aRespuesta(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(), cliente.getRut(), cliente.getNombre(), cliente.getEmail(),
                cliente.getTelefono(), cliente.getSegmento(), cliente.getActivo(),
                cliente.getFechaActualizacion());
    }
}
