# Despliegue local y propuesta AWS

## Despliegue local con Docker Compose

La entrega usa un Dockerfile multi-stage. La primera etapa compila el proyecto Maven completo y las etapas finales producen una imagen pequeña por aplicación. `compose.yaml` conecta once componentes en una red interna.

Inicio normal:

```powershell
.\iniciar-todo.cmd
```

Inicio manual:

```powershell
docker compose build
docker compose up -d
docker compose ps
```

Detención:

```powershell
docker compose down
```

## Variables de entorno principales

| Variable | Uso |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexión PostgreSQL |
| `CONFIG_SERVER_URL` | Dirección de Config Server |
| `EUREKA_URL` | Registro de servicios |
| `KAFKA_BOOTSTRAP_SERVERS` | Broker Kafka |
| `JWT_ISSUER_URI`, `JWT_JWK_SET_URI` | Validación de JWT |
| `ATM_API_KEY` | Control adicional del BFF ATM |
| `GATEWAY_KEYSTORE_PASSWORD` | Clave del certificado local |
| `BATCH_CHUNK_SIZE`, `BATCH_POOL_SIZE` | Rendimiento de Spring Batch |

Las claves que aparecen en Compose son de demostración local. No se deben usar como secretos reales.

## Escalamiento horizontal

Los servicios sin estado pueden levantar réplicas adicionales:

```powershell
docker compose up -d --scale cuentas-service=2 --scale transacciones-service=2 --scale clientes-service=2
```

Cada contenedor se registra con un identificador distinto. Eureka mantiene las instancias disponibles y Spring Cloud LoadBalancer distribuye las llamadas por nombre lógico.

## Propuesta de despliegue en AWS

| Componente local | Servicio AWS propuesto | Motivo |
|---|---|---|
| Contenedores Spring Boot | ECS Fargate o EKS | Escalamiento y reemplazo automático |
| PostgreSQL | RDS PostgreSQL Multi-AZ | Respaldo, failover y mantenimiento administrado |
| Kafka | Amazon MSK | Broker administrado y alta disponibilidad |
| Gateway | Application Load Balancer + API Gateway | Entrada HTTPS, límites y observabilidad |
| Certificado PKCS12 | AWS Certificate Manager | Renovación administrada |
| Claves de Compose | Secrets Manager | Rotación y acceso por rol IAM |
| Imágenes | Amazon ECR | Registro privado y versionado |
| Logs y métricas | CloudWatch | Alarmas, trazas y paneles |
| Archivos CSV | Amazon S3 | Entrada durable para los jobs Batch |
| Configuración | Parameter Store o repositorio Git privado | Separación entre código y ambiente |

## Secuencia sugerida en AWS

1. Publicar las imágenes versionadas en ECR.
2. Crear RDS PostgreSQL y ejecutar los scripts de `database/init`.
3. Crear el clúster MSK y los tópicos `banco.retiros` y `banco.pagos`.
4. Guardar claves y cadenas de conexión en Secrets Manager.
5. Crear tareas ECS para Config Server, Eureka, servicios, BFF, Batch y Gateway.
6. Publicar solamente el Gateway detrás de un Load Balancer con certificado ACM.
7. Configurar auto scaling por CPU, memoria y cantidad de solicitudes.
8. Enviar logs a CloudWatch y crear alarmas de salud, errores y circuitos abiertos.

## Consideraciones de producción

- Keycloak debe usar una base externa, varias instancias y un administrador no temporal.
- El certificado autofirmado debe reemplazarse por uno válido.
- Las contraseñas deben salir del repositorio y rotarse.
- Kafka debe usar cifrado y autenticación.
- Los endpoints Actuator deben restringirse a la red de administración.
- La base debe usar copias de seguridad, cifrado y Multi-AZ.
- Los jobs Batch deben leer desde S3 y ejecutarse de forma programada con EventBridge o una tarea ECS.
- Las pruebas de carga deben definir el tamaño final de chunks, pool y réplicas.
