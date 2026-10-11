CREATE TABLE IF NOT EXISTS clientes (
    id BIGSERIAL PRIMARY KEY,
    rut VARCHAR(15) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    telefono VARCHAR(30),
    segmento VARCHAR(30) NOT NULL CHECK (segmento IN ('PERSONA', 'PYME', 'PREMIUM')),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cuentas (
    id BIGINT PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    saldo NUMERIC(15, 2) NOT NULL CHECK (saldo >= 0),
    edad INTEGER NOT NULL CHECK (edad BETWEEN 18 AND 120),
    tipo VARCHAR(30) NOT NULL CHECK (tipo IN ('AHORRO', 'PRESTAMO', 'HIPOTECA')),
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    moneda VARCHAR(3) NOT NULL DEFAULT 'CLP',
    version BIGINT NOT NULL DEFAULT 0,
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS transacciones (
    id BIGSERIAL PRIMARY KEY,
    cuenta_id BIGINT NOT NULL REFERENCES cuentas(id),
    fecha DATE NOT NULL,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('CREDITO', 'DEBITO')),
    monto NUMERIC(15, 2) NOT NULL CHECK (monto > 0),
    descripcion VARCHAR(255),
    canal VARCHAR(30) NOT NULL DEFAULT 'LEGACY',
    anomalia BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_transacciones_cuenta_fecha
    ON transacciones(cuenta_id, fecha DESC);

CREATE TABLE IF NOT EXISTS eventos_procesados (
    evento_id VARCHAR(80) PRIMARY KEY,
    tipo_evento VARCHAR(50) NOT NULL,
    procesado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS pagos (
    id BIGSERIAL PRIMARY KEY,
    operacion_id VARCHAR(80) NOT NULL UNIQUE,
    tipo VARCHAR(30) NOT NULL CHECK (tipo IN ('PAGO', 'TRANSFERENCIA', 'DEPOSITO')),
    cuenta_origen_id BIGINT REFERENCES cuentas(id),
    cuenta_destino_id BIGINT REFERENCES cuentas(id),
    monto NUMERIC(15, 2) NOT NULL CHECK (monto > 0),
    descripcion VARCHAR(255) NOT NULL,
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('APROBADO', 'RECHAZADO', 'COMPENSADO')),
    fecha TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pagos_fecha ON pagos(fecha DESC);

CREATE TABLE IF NOT EXISTS batch_transacciones_diarias (
    legacy_id BIGINT PRIMARY KEY,
    fecha DATE NOT NULL,
    monto NUMERIC(15, 2) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    anomalia BOOLEAN NOT NULL,
    procesado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS batch_intereses_mensuales (
    id BIGSERIAL PRIMARY KEY,
    cuenta_id BIGINT NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    saldo NUMERIC(15, 2) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    tasa NUMERIC(8, 5) NOT NULL,
    interes_calculado NUMERIC(15, 2) NOT NULL,
    periodo VARCHAR(7) NOT NULL,
    procesado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (cuenta_id, periodo)
);

CREATE TABLE IF NOT EXISTS batch_movimientos_anuales (
    id BIGSERIAL PRIMARY KEY,
    fuente_hash VARCHAR(64) NOT NULL UNIQUE,
    cuenta_id BIGINT NOT NULL,
    fecha DATE NOT NULL,
    transaccion VARCHAR(30) NOT NULL,
    monto NUMERIC(15, 2) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    procesado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_batch_movimientos_cuenta_fecha
    ON batch_movimientos_anuales(cuenta_id, fecha);

CREATE TABLE IF NOT EXISTS batch_estados_cuenta_anuales (
    cuenta_id BIGINT NOT NULL,
    anio INTEGER NOT NULL,
    cantidad_movimientos BIGINT NOT NULL,
    total_depositos NUMERIC(15, 2) NOT NULL,
    total_cargos NUMERIC(15, 2) NOT NULL,
    saldo_neto NUMERIC(15, 2) NOT NULL,
    generado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (cuenta_id, anio)
);

CREATE TABLE IF NOT EXISTS batch_errores (
    id BIGSERIAL PRIMARY KEY,
    job_nombre VARCHAR(100) NOT NULL,
    etapa VARCHAR(30) NOT NULL,
    dato_original TEXT,
    mensaje_error VARCHAR(500) NOT NULL,
    registrado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
