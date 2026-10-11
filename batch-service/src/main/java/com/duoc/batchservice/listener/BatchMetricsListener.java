package com.duoc.batchservice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

import java.time.Duration;
import java.time.LocalDateTime;

public class BatchMetricsListener implements JobExecutionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchMetricsListener.class);

    @Override
    public void beforeJob(JobExecution jobExecution) {
        LOGGER.info("Iniciando job {} con parametros {}",
                jobExecution.getJobInstance().getJobName(), jobExecution.getJobParameters());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        LocalDateTime inicio = jobExecution.getStartTime();
        LocalDateTime termino = jobExecution.getEndTime();
        long duracion = inicio != null && termino != null
                ? Duration.between(inicio, termino).toMillis()
                : -1;
        LOGGER.info("Job {} finalizo con estado {} en {} ms",
                jobExecution.getJobInstance().getJobName(), jobExecution.getStatus(), duracion);
    }
}
