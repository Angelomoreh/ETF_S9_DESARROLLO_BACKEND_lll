package com.duoc.clientesservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 15)
    private String rut;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(length = 30)
    private String telefono;

    @Column(nullable = false, length = 30)
    private String segmento;

    @Column(nullable = false)
    private Boolean activo;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    protected Cliente() {
    }

    public Cliente(String rut, String nombre, String email, String telefono, String segmento) {
        this.rut = rut;
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.segmento = segmento;
        this.activo = true;
    }

    @PrePersist
    @PreUpdate
    void actualizarFecha() {
        fechaActualizacion = OffsetDateTime.now();
    }

    public void actualizar(String nombre, String email, String telefono, String segmento) {
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.segmento = segmento;
    }

    public void desactivar() {
        this.activo = false;
    }

    public Long getId() { return id; }
    public String getRut() { return rut; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public String getTelefono() { return telefono; }
    public String getSegmento() { return segmento; }
    public Boolean getActivo() { return activo; }
    public OffsetDateTime getFechaActualizacion() { return fechaActualizacion; }
}
