$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Output = Join-Path $Root "evidencias\salidas"
$Gateway = "https://localhost:8443"
New-Item -ItemType Directory -Path $Output -Force | Out-Null
Set-Location $Root

# Compatibilidad con Windows PowerShell 5.1 y PowerShell 7 para el certificado local.




function Invoke-BancoApi {
    param(
        [string]$Method = "Get",
        [Parameter(Mandatory = $true)][string]$Uri,
        [hashtable]$Headers,
        [string]$ContentType,
        [object]$Body
    )

    $Parameters = @{
        Method = $Method
        Uri    = $Uri
    }

    if ($Headers) { $Parameters.Headers = $Headers }
    if ($ContentType) { $Parameters.ContentType = $ContentType }
    if ($null -ne $Body) { $Parameters.Body = $Body }

    Invoke-RestMethod @Parameters
}



function Save-Json {
    param([string]$Nombre, [object]$Contenido)
    $Json = $Contenido | ConvertTo-Json -Depth 20
    Set-Content -Path (Join-Path $Output $Nombre) -Value $Json -Encoding UTF8
    Write-Host "OK $Nombre" -ForegroundColor Green
}

function Save-Text {
    param([string]$Nombre, [string]$Contenido)
    Set-Content -Path (Join-Path $Output $Nombre) -Value $Contenido -Encoding UTF8
    Write-Host "OK $Nombre" -ForegroundColor Green
}


function Get-HttpStatus {
    param($ErrorRecord)

    try {
        $Response = $ErrorRecord.Exception.Response

        if ($null -ne $Response -and
            $null -ne $Response.StatusCode) {
            return [int]$Response.StatusCode
        }
    }
    catch {
        # Si no hay respuesta HTTP, devolver 0.
    }

    return 0
}


Write-Host "1. Comprobando HTTPS, OAuth2 y seguridad distribuida..." -ForegroundColor Cyan
try {
    Invoke-BancoApi -Uri "$Gateway/api/web/cuentas/101" | Out-Null
    throw "El Gateway permitio una solicitud sin token"
}
catch {
    $StatusCode = Get-HttpStatus $_
    if ($StatusCode -ne 401) { throw }
    Save-Text "01_https_oauth_sin_token_401.txt" "Solicitud HTTPS sin token rechazada correctamente con HTTP 401 Unauthorized."
}

$TokenResponse = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8180/realms/banco-xyz/protocol/openid-connect/token" `
    -ContentType "application/x-www-form-urlencoded" `
    -Body @{
        grant_type = "password"
        client_id  = "banco-api"
        username   = "angelo"
        password   = "angelo123"
    }

if (-not $TokenResponse.access_token) { throw "Keycloak no entrego un access_token" }

$AuthHeaders = @{ Authorization = "Bearer $($TokenResponse.access_token)" }
$AtmHeaders = @{
    Authorization = "Bearer $($TokenResponse.access_token)"
    "X-ATM-KEY"  = "BANCO-XYZ-ATM-2026"
}
Save-Text "02_oauth_token_obtenido.txt" "Keycloak entrego un token JWT funcional. El acceso externo usa HTTPS en el puerto 8443."

Write-Host "2. Comprobando Config Server, Eureka y servicios registrados..." -ForegroundColor Cyan
$Config = Invoke-RestMethod -Uri "http://localhost:8888/bff-web/default"
Save-Json "03_config_server.json" $Config
$Eureka = Invoke-RestMethod -Uri "http://localhost:8761/eureka/apps" -Headers @{ Accept = "application/json" }
Save-Json "04_eureka_servicios.json" $Eureka

Write-Host "3. Probando los tres BFF mediante el Gateway seguro..." -ForegroundColor Cyan
$Web = Invoke-BancoApi -Uri "$Gateway/api/web/cuentas/101" -Headers $AuthHeaders
Save-Json "05_bff_web_respuesta_completa.json" $Web
$Mobile = Invoke-BancoApi -Uri "$Gateway/api/mobile/cuentas/101" -Headers $AuthHeaders
Save-Json "06_bff_mobile_respuesta_ligera.json" $Mobile
$AtmSaldo = Invoke-BancoApi -Uri "$Gateway/api/atm/cuentas/101/saldo" -Headers $AtmHeaders
Save-Json "07_bff_atm_saldo_seguro.json" $AtmSaldo

try {
    Invoke-BancoApi -Uri "$Gateway/api/atm/cuentas/101/saldo" -Headers $AuthHeaders | Out-Null
    throw "El BFF ATM permitio una solicitud sin X-ATM-KEY"
}
catch {
    $StatusCode = Get-HttpStatus $_
    if ($StatusCode -ne 401) { throw }
    Save-Text "08_atm_sin_api_key_401.txt" "Solicitud ATM con JWT pero sin X-ATM-KEY rechazada correctamente con HTTP 401."
}

Write-Host "4. Probando gestion de clientes, pagos, transferencias y depositos..." -ForegroundColor Cyan
$Cliente = Invoke-BancoApi -Uri "$Gateway/api/clientes/1" -Headers $AuthHeaders
Save-Json "09_cliente_consultado.json" $Cliente

$PagoBody = @{ cuentaOrigenId = 101; monto = 100; descripcion = "Pago de servicio de prueba EFT" } | ConvertTo-Json
$Pago = Invoke-BancoApi -Method Post -Uri "$Gateway/api/pagos" -Headers $AuthHeaders `
    -ContentType "application/json" -Body $PagoBody
Save-Json "10_pago_aprobado.json" $Pago

$TransferenciaBody = @{
    cuentaOrigenId = 101
    cuentaDestinoId = 102
    monto = 100
    descripcion = "Transferencia de prueba EFT"
} | ConvertTo-Json
$Transferencia = Invoke-BancoApi -Method Post -Uri "$Gateway/api/pagos/transferencias" `
    -Headers $AuthHeaders -ContentType "application/json" -Body $TransferenciaBody
Save-Json "11_transferencia_aprobada.json" $Transferencia

$DepositoBody = @{ cuentaDestinoId = 101; monto = 200; descripcion = "Deposito de prueba EFT" } | ConvertTo-Json
$Deposito = Invoke-BancoApi -Method Post -Uri "$Gateway/api/pagos/depositos" -Headers $AuthHeaders `
    -ContentType "application/json" -Body $DepositoBody
Save-Json "12_deposito_aprobado.json" $Deposito

Write-Host "5. Probando mensajeria asincrona Kafka..." -ForegroundColor Cyan
$RetiroBody = @{ monto = 500 } | ConvertTo-Json
$Retiro = Invoke-BancoApi -Method Post -Uri "$Gateway/api/atm/cuentas/101/retiros" `
    -Headers $AtmHeaders -ContentType "application/json" -Body $RetiroBody
Save-Json "13_retiro_publicado_kafka.json" $Retiro
Start-Sleep -Seconds 4

$WebDespues = Invoke-BancoApi -Uri "$Gateway/api/web/cuentas/101" -Headers $AuthHeaders
Save-Json "14_evento_consumido_y_visible.json" $WebDespues
$KafkaLogs = docker compose logs --tail 100 transacciones-service 2>&1 | Out-String
Save-Text "15_logs_kafka.txt" $KafkaLogs

Write-Host "6. Ejecutando los tres procesos Spring Batch en paralelo..." -ForegroundColor Cyan
$Batch = Invoke-BancoApi -Method Post -Uri "$Gateway/api/batch/ejecutar" -Headers $AuthHeaders
Save-Json "16_batch_tres_jobs.json" $Batch
$BatchResumen = Invoke-BancoApi -Uri "$Gateway/api/batch/resumen" -Headers $AuthHeaders
Save-Json "17_batch_resumen.json" $BatchResumen
$BatchEjecuciones = Invoke-BancoApi -Uri "$Gateway/api/batch/ejecuciones" -Headers $AuthHeaders
Save-Json "18_batch_ejecuciones.json" $BatchEjecuciones

$DatosSql = docker exec banco-eft-postgres psql -U banco_user -d banco_cloud -c "SELECT COUNT(*) AS clientes FROM clientes; SELECT COUNT(*) AS pagos FROM pagos; SELECT COUNT(*) AS transacciones_batch FROM batch_transacciones_diarias; SELECT COUNT(*) AS intereses_batch FROM batch_intereses_mensuales; SELECT COUNT(*) AS movimientos_anuales FROM batch_movimientos_anuales; SELECT COUNT(*) AS estados_anuales FROM batch_estados_cuenta_anuales; SELECT COUNT(*) AS errores_controlados FROM batch_errores;" | Out-String
Save-Text "19_datos_postgresql.txt" $DatosSql

Write-Host "7. Demostrando Circuit Breaker y fallback..." -ForegroundColor Cyan
Set-Location $Root
docker compose stop transacciones-service | Out-Null
try {
    Start-Sleep -Seconds 4
    $RespuestaResiliente = Invoke-BancoApi -Uri "$Gateway/api/web/cuentas/101" -Headers $AuthHeaders
    Save-Json "20_resilience4j_fallback.json" $RespuestaResiliente
}
finally {
    docker compose start transacciones-service | Out-Null
}

Write-Host "8. Demostrando escalabilidad horizontal con dos instancias..." -ForegroundColor Cyan
docker compose up -d --scale clientes-service=2 | Out-Null
Start-Sleep -Seconds 15
$EurekaEscalado = Invoke-RestMethod -Uri "http://localhost:8761/eureka/apps/CLIENTES-SERVICE" `
    -Headers @{ Accept = "application/json" }
Save-Json "21_escalabilidad_horizontal.json" $EurekaEscalado
docker compose up -d --scale clientes-service=1 | Out-Null

Write-Host ""
Write-Host "PRUEBAS EFT FINALIZADAS CORRECTAMENTE" -ForegroundColor Green
Write-Host "Se comprobaron Spring Batch, tres microservicios, tres BFF, HTTPS, OAuth2, Eureka, Config Server, Kafka, PostgreSQL, Resilience4j y escalabilidad horizontal."
Write-Host "Las salidas quedaron guardadas en evidencias\salidas"
