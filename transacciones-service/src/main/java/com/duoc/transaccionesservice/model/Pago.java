package com.duoc.transaccionesservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operacion_id", nullable = false, unique = true, length = 80)
    private String operacionId;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(name = "cuenta_origen_id")
    private Long cuentaOrigenId;

    @Column(name = "cuenta_destino_id")
    private Long cuentaDestinoId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 255)
    private String descripcion;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(nullable = false)
    private OffsetDateTime fecha;

    protected Pago() {
    }

    public Pago(
            String operacionId,
            String tipo,
            Long cuentaOrigenId,
            Long cuentaDestinoId,
            BigDecimal monto,
            String descripcion,
            String estado,
            OffsetDateTime fecha
    ) {
        this.operacionId = operacionId;
        this.tipo = tipo;
        this.cuentaOrigenId = cuentaOrigenId;
        this.cuentaDestinoId = cuentaDestinoId;
        this.monto = monto;
        this.descripcion = descripcion;
        this.estado = estado;
        this.fecha = fecha;
    }

    public String getOperacionId() { return operacionId; }
    public String getTipo() { return tipo; }
    public Long getCuentaOrigenId() { return cuentaOrigenId; }
    public Long getCuentaDestinoId() { return cuentaDestinoId; }
    public BigDecimal getMonto() { return monto; }
    public String getDescripcion() { return descripcion; }
    public String getEstado() { return estado; }
    public OffsetDateTime getFecha() { return fecha; }
}
