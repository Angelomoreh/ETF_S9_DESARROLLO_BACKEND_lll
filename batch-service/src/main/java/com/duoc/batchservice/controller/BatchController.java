package com.duoc.batchservice.controller;

import com.duoc.batchservice.dto.EjecucionBatchResponse;
import com.duoc.batchservice.dto.ResumenBatchResponse;
import com.duoc.batchservice.service.BatchConsultaService;
import com.duoc.batchservice.service.BatchOrchestratorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/batch")
public class BatchController {

    private final BatchOrchestratorService orchestrator;
    private final BatchConsultaService consultas;

    public BatchController(BatchOrchestratorService orchestrator, BatchConsultaService consultas) {
        this.orchestrator = orchestrator;
        this.consultas = consultas;
    }

    @GetMapping("/jobs")
    public Map<String, Object> jobs() {
        return Map.of(
                "jobs", List.of("transaccionesJob", "interesesJob", "cuentasAnualesJob"),
                "paralelismo", "Los tres jobs son lanzados por un pool de tres hilos",
                "recuperacion", "Cada job dispone de hasta tres intentos y conserva metadata de reinicio",
                "origen", "Archivos CSV legacy de la semana 3"
        );
    }

    @PostMapping("/ejecutar")
    public EjecucionBatchResponse ejecutar() {
        return orchestrator.ejecutarTodos();
    }

    @GetMapping("/resumen")
    public ResumenBatchResponse resumen() {
        return consultas.resumen();
    }

    @GetMapping("/ejecuciones")
    public List<Map<String, Object>> ejecuciones() {
        return consultas.ejecuciones();
    }
}
