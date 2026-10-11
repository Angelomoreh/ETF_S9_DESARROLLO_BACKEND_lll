package com.duoc.clientesservice.repository;

import com.duoc.clientesservice.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByRutIgnoreCase(String rut);

    boolean existsByEmailIgnoreCase(String email);
}
