# Banco XYZ Backend Avanzado

## Evaluacion Final Transversal

**Asignatura:** Desarrollo Backend III  
**Estudiante:** Angelo Silva  
**Semana:** 9  


## Resumen del proyecto

Este proyecto moderniza los procesos legacy del Banco XYZ mediante una solucion compuesta por Spring Batch, microservicios, tres Backend for Frontend y servicios de infraestructura de Spring Cloud.

La solucion procesa los archivos CSV heredados, separa las funciones de cuentas, pagos y clientes, adapta las respuestas para los canales Web, Mobile y ATM, y utiliza OAuth2, HTTPS, Kafka y Resilience4j para proteger y mantener disponible el sistema. Todos los componentes se ejecutan con Docker Compose y se registran dinamicamente en Eureka.

## Procesos clave de la migracion

Los cinco procesos considerados en la propuesta son:

1. Migracion de procesos legacy a Spring Batch.
2. Division del sistema monolitico en microservicios.
3. Implementacion del patron Backend for Frontend para tres canales.
4. Seguridad distribuida mediante Gateway, HTTPS, OAuth2 y una clave adicional para ATM.
5. Mensajeria asincrona con Apache Kafka.



## Componentes

| Componente | Puerto | Funcion |
|---|---:|---|
| API Gateway | 8443 HTTPS | Entrada segura, validacion JWT y enrutamiento |
| Keycloak | 8180 | Emision de tokens OAuth2 |
| Eureka | 8761 | Registro y descubrimiento dinamico |
| Config Server | 8888 | Configuracion centralizada |
| PostgreSQL | 5433 | Persistencia operacional y metadata Batch |
| Kafka | interno | Eventos de retiros y pagos |
| cuentas-service | interno | Apertura logica, saldos y movimientos de cuentas |
| transacciones-service | interno | Pagos, transferencias, depositos y transacciones |
| clientes-service | interno | Perfiles y datos de clientes |
| batch-service | interno | Tres procesos Spring Batch sobre datos legacy |
| bff-web | interno | Respuesta completa para navegadores |
| bff-mobile | interno | Respuesta ligera con los ultimos movimientos |
| bff-atm | interno | Consulta de saldo y retiro con doble validacion |

## Procesamiento Spring Batch

El modulo `batch-service` contiene tres jobs independientes:

| Job | Archivo de entrada | Resultado |
|---|---|---|
| `transaccionesJob` | `transacciones.csv` | Transacciones validadas y deteccion de montos anomalos |
| `interesesJob` | `intereses.csv` | Calculo mensual segun tipo de cuenta |
| `cuentasAnualesJob` | `cuentas_anuales.csv` | Movimientos depurados y estados anuales por cuenta |

Cada job trabaja por bloques de 100 registros, permite hasta tres reintentos ante fallos temporales, omite datos invalidos de forma controlada y registra el detalle en `batch_errores`. Los tres jobs se lanzan en paralelo mediante un pool de tres hilos. Spring Batch conserva su metadata en PostgreSQL, por lo que una ejecucion fallida puede continuar desde el ultimo punto confirmado.

Los datos utilizados se encuentran en `batch-service/src/main/resources/legacy` y corresponden al conjunto legacy entregado para el ramo.

## Diferencias entre los BFF

- **Web:** entrega cuenta, resumen y lista completa de transacciones.
- **Mobile:** reduce la respuesta a los datos esenciales y los ultimos cinco movimientos.
- **ATM:** solamente permite saldo y retiro. Ademas del JWT exige la cabecera `X-ATM-KEY`.

La separacion evita entregar informacion innecesaria y permite que cada canal evolucione sin afectar a los demas.

## Seguridad

El acceso externo utiliza `https://localhost:8443`. El certificado incluido es autofirmado y se usa unicamente para demostrar TLS en el entorno local.

Keycloak importa automaticamente el realm `banco-xyz` con estas credenciales de demostracion:

| Dato | Valor |
|---|---|
| Client ID | `banco-api` |
| Usuario | `angelo` |
| Clave | `angelo123` |
| Clave ATM | `BANCO-XYZ-ATM-2026` |
| Administrador Keycloak | `admin` / `admin` |

Estas claves no deben usarse en produccion. En AWS se reemplazan por Secrets Manager y certificados de ACM, como se explica en `despliegue.md`.

## Resiliencia y consistencia

- Resilience4j protege las llamadas de los BFF y del servicio de pagos.
- Si transacciones deja de responder, Web y Mobile mantienen disponible la informacion de la cuenta mediante fallback.
- Los retiros se publican en Kafka y se procesan con una tabla de idempotencia para evitar duplicados.
- Las transferencias aplican compensacion: si no se puede acreditar el destino, se devuelve el monto a la cuenta de origen.
- Las cuentas utilizan bloqueo pesimista y versionado para impedir actualizaciones simultaneas inconsistentes.
- Los servicios no publican puertos propios; solamente el Gateway queda expuesto al cliente.


## Escalabilidad horizontal

Los tres microservicios de negocio no tienen un nombre fijo de contenedor. Esto permite iniciar varias instancias y dejar que Eureka y Spring Cloud LoadBalancer distribuyan las solicitudes.

Ejemplo:

```powershell
docker compose up -d --scale cuentas-service=2 --scale transacciones-service=2 --scale clientes-service=2
```

Después se pueden observar las instancias desde <http://localhost:8761>.

## Pruebas incluidas

El script `probar-apis.cmd` comprueba:

- rechazo HTTP 401 sin token;
- conexion HTTPS y token OAuth2;
- Config Server y Eureka;
- los tres BFF;
- proteccion adicional del ATM;
- gestion de clientes;
- pagos, transferencias y depositos;
- publicacion y consumo de eventos Kafka;
- ejecucion paralela de los tres jobs Batch;
- datos persistidos en PostgreSQL;
- fallback de Resilience4j;
- dos instancias simultaneas de `clientes-service`.

Tambien se incluyen pruebas unitarias para cuentas, transacciones, pagos, clientes y procesadores Batch.

## Estructura principal

```text
Angelo_Silva_PBY2203_EFT
|-- api-gateway
|-- batch-service
|-- bff-atm
|-- bff-mobile
|-- bff-web
|-- clientes-service
|-- config-repo
|-- config-server
|-- cuentas-service
|-- database
|-- discovery-server
|-- documentacion
|-- evidencias
|-- postman
|-- scripts
|-- security
|-- transacciones-service
|-- despliegue.md
|-- instrucciones.md
|-- GUION_VIDEO.md
|-- INFORME_TECNICO.pdf
|-- compose.yaml
|-- Dockerfile
`-- pom.xml
```
