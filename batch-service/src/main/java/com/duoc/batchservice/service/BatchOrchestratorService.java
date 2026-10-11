package com.duoc.batchservice.service;

import com.duoc.batchservice.dto.EjecucionBatchResponse;
import com.duoc.batchservice.dto.ResultadoJob;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class BatchOrchestratorService {

    private final JobLauncher jobLauncher;
    private final Job transaccionesJob;
    private final Job interesesJob;
    private final Job cuentasAnualesJob;
    private final ThreadPoolTaskExecutor executor;
    private final int maxReintentos;

    public BatchOrchestratorService(
            JobLauncher jobLauncher,
            @Qualifier("transaccionesJob") Job transaccionesJob,
            @Qualifier("interesesJob") Job interesesJob,
            @Qualifier("cuentasAnualesJob") Job cuentasAnualesJob,
            @Qualifier("batchParallelExecutor") ThreadPoolTaskExecutor executor,
            @Value("${banco.batch.max-reintentos}") int maxReintentos
    ) {
        this.jobLauncher = jobLauncher;
        this.transaccionesJob = transaccionesJob;
        this.interesesJob = interesesJob;
        this.cuentasAnualesJob = cuentasAnualesJob;
        this.executor = executor;
        this.maxReintentos = maxReintentos;
    }

    public EjecucionBatchResponse ejecutarTodos() {
        OffsetDateTime inicio = OffsetDateTime.now();
        String ejecucionId = UUID.randomUUID().toString();
        long instante = System.currentTimeMillis();

        List<CompletableFuture<ResultadoJob>> tareas = List.of(
                ejecutarEnParalelo(transaccionesJob, ejecucionId, instante),
                ejecutarEnParalelo(interesesJob, ejecucionId, instante),
                ejecutarEnParalelo(cuentasAnualesJob, ejecucionId, instante)
        );

        List<ResultadoJob> resultados = tareas.stream().map(CompletableFuture::join).toList();
        boolean completado = resultados.stream().allMatch(resultado -> "COMPLETED".equals(resultado.estado()));
        return new EjecucionBatchResponse(
                ejecucionId,
                completado ? "COMPLETADO" : "CON_ERRORES",
                inicio,
                OffsetDateTime.now(),
                true,
                resultados
        );
    }

    private CompletableFuture<ResultadoJob> ejecutarEnParalelo(Job job, String ejecucionId, long instante) {
        return CompletableFuture.supplyAsync(() -> ejecutarConReintento(job, ejecucionId, instante), executor);
    }

    private ResultadoJob ejecutarConReintento(Job job, String ejecucionId, long instante) {
        JobExecution execution = null;
        int intento = 0;
        Exception ultimoError = null;
        while (intento < maxReintentos) {
            intento++;
            try {
                JobParameters parametros = new JobParametersBuilder()
                        .addString("ejecucionId", ejecucionId)
                        .addLong("instante", instante)
                        .toJobParameters();
                execution = jobLauncher.run(job, parametros);
                if (execution.getStatus() == BatchStatus.COMPLETED) {
                    return aResultado(execution, intento);
                }
            } catch (Exception error) {
                ultimoError = error;
            }
        }

        if (execution != null) {
            return aResultado(execution, intento);
        }
        throw new IllegalStateException(
                "No fue posible iniciar " + job.getName() + " despues de " + intento + " intentos",
                ultimoError);
    }

    private ResultadoJob aResultado(JobExecution execution, int intentos) {
        long leidos = execution.getStepExecutions().stream().mapToLong(StepExecution::getReadCount).sum();
        long escritos = execution.getStepExecutions().stream().mapToLong(StepExecution::getWriteCount).sum();
        long omitidos = execution.getStepExecutions().stream()
                .mapToLong(step -> step.getReadSkipCount() + step.getProcessSkipCount() + step.getWriteSkipCount())
                .sum();
        long duracion = execution.getStartTime() != null && execution.getEndTime() != null
                ? Duration.between(
                        execution.getStartTime().atOffset(ZoneOffset.UTC),
                        execution.getEndTime().atOffset(ZoneOffset.UTC)).toMillis()
                : -1;
        return new ResultadoJob(
                execution.getJobInstance().getJobName(),
                execution.getStatus().name(),
                leidos,
                escritos,
                omitidos,
                duracion,
                intentos
        );
    }
}
