package com.duoc.transaccionesservice.repository;

import com.duoc.transaccionesservice.model.Pago;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findAllByOrderByFechaDesc(Pageable pageable);
}
