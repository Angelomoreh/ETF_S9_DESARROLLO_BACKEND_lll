package com.duoc.batchservice.config;

import com.duoc.batchservice.listener.BatchMetricsListener;
import com.duoc.batchservice.listener.RegistroSkipListener;
import com.duoc.batchservice.model.InteresCalculado;
import com.duoc.batchservice.model.LegacyInteres;
import com.duoc.batchservice.model.LegacyMovimiento;
import com.duoc.batchservice.model.LegacyTransaccion;
import com.duoc.batchservice.model.MovimientoAnual;
import com.duoc.batchservice.model.TransaccionProcesada;
import com.duoc.batchservice.processor.InteresProcessor;
import com.duoc.batchservice.processor.MovimientoAnualProcessor;
import com.duoc.batchservice.processor.TransaccionProcessor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class BatchJobConfig {

    @Bean
    TransaccionProcessor transaccionProcessor() {
        return new TransaccionProcessor();
    }

    @Bean
    InteresProcessor interesProcessor() {
        return new InteresProcessor();
    }

    @Bean
    MovimientoAnualProcessor movimientoAnualProcessor() {
        return new MovimientoAnualProcessor();
    }

    @Bean
    FlatFileItemReader<LegacyTransaccion> transaccionesReader() {
        return new FlatFileItemReaderBuilder<LegacyTransaccion>()
                .name("transaccionesLegacyReader")
                .resource(new ClassPathResource("legacy/transacciones.csv"))
                .linesToSkip(1)
                .delimited()
                .names("id", "fecha", "monto", "tipo")
                .fieldSetMapper(campos -> new LegacyTransaccion(
                        campos.readString("id"), campos.readString("fecha"),
                        campos.readString("monto"), campos.readString("tipo")))
                .build();
    }

    @Bean
    FlatFileItemReader<LegacyInteres> interesesReader() {
        return new FlatFileItemReaderBuilder<LegacyInteres>()
                .name("interesesLegacyReader")
                .resource(new ClassPathResource("legacy/intereses.csv"))
                .linesToSkip(1)
                .delimited()
                .names("cuenta_id", "nombre", "saldo", "edad", "tipo")
                .fieldSetMapper(campos -> new LegacyInteres(
                        campos.readString("cuenta_id"), campos.readString("nombre"),
                        campos.readString("saldo"), campos.readString("edad"),
                        campos.readString("tipo")))
                .build();
    }

    @Bean
    FlatFileItemReader<LegacyMovimiento> cuentasAnualesReader() {
        return new FlatFileItemReaderBuilder<LegacyMovimiento>()
                .name("cuentasAnualesLegacyReader")
                .resource(new ClassPathResource("legacy/cuentas_anuales.csv"))
                .linesToSkip(1)
                .delimited()
                .names("cuenta_id", "fecha", "transaccion", "monto", "descripcion")
                .fieldSetMapper(campos -> new LegacyMovimiento(
                        campos.readString("cuenta_id"), campos.readString("fecha"),
                        campos.readString("transaccion"), campos.readString("monto"),
                        campos.readString("descripcion")))
                .build();
    }

    @Bean
    JdbcBatchItemWriter<TransaccionProcesada> transaccionesWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<TransaccionProcesada>()
                .dataSource(dataSource)
                .sql("""
                        INSERT INTO batch_transacciones_diarias
                            (legacy_id, fecha, monto, tipo, anomalia, procesado_en)
                        VALUES (:legacyId, :fecha, :monto, :tipo, :anomalia, CURRENT_TIMESTAMP)
                        ON CONFLICT (legacy_id) DO UPDATE SET
                            fecha = EXCLUDED.fecha,
                            monto = EXCLUDED.monto,
                            tipo = EXCLUDED.tipo,
                            anomalia = EXCLUDED.anomalia,
                            procesado_en = CURRENT_TIMESTAMP
                        """)
                .itemSqlParameterSourceProvider(item -> new MapSqlParameterSource()
                        .addValue("legacyId", item.legacyId())
                        .addValue("fecha", item.fecha())
                        .addValue("monto", item.monto())
                        .addValue("tipo", item.tipo())
                        .addValue("anomalia", item.anomalia()))
                .build();
    }

    @Bean
    JdbcBatchItemWriter<InteresCalculado> interesesWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<InteresCalculado>()
                .dataSource(dataSource)
                .sql("""
                        INSERT INTO batch_intereses_mensuales
                            (cuenta_id, nombre, saldo, tipo, tasa, interes_calculado, periodo, procesado_en)
                        VALUES (:cuentaId, :nombre, :saldo, :tipo, :tasa, :interes, :periodo, CURRENT_TIMESTAMP)
                        ON CONFLICT (cuenta_id, periodo) DO UPDATE SET
                            nombre = EXCLUDED.nombre,
                            saldo = EXCLUDED.saldo,
                            tipo = EXCLUDED.tipo,
                            tasa = EXCLUDED.tasa,
                            interes_calculado = EXCLUDED.interes_calculado,
                            procesado_en = CURRENT_TIMESTAMP
                        """)
                .itemSqlParameterSourceProvider(item -> new MapSqlParameterSource()
                        .addValue("cuentaId", item.cuentaId())
                        .addValue("nombre", item.nombre())
                        .addValue("saldo", item.saldo())
                        .addValue("tipo", item.tipo())
                        .addValue("tasa", item.tasa())
                        .addValue("interes", item.interesCalculado())
                        .addValue("periodo", item.periodo()))
                .build();
    }

    @Bean
    JdbcBatchItemWriter<MovimientoAnual> cuentasAnualesWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<MovimientoAnual>()
                .dataSource(dataSource)
                .sql("""
                        INSERT INTO batch_movimientos_anuales
                            (fuente_hash, cuenta_id, fecha, transaccion, monto, descripcion, procesado_en)
                        VALUES (:hash, :cuentaId, :fecha, :transaccion, :monto, :descripcion, CURRENT_TIMESTAMP)
                        ON CONFLICT (fuente_hash) DO UPDATE SET procesado_en = CURRENT_TIMESTAMP
                        """)
                .itemSqlParameterSourceProvider(item -> new MapSqlParameterSource()
                        .addValue("hash", item.fuenteHash())
                        .addValue("cuentaId", item.cuentaId())
                        .addValue("fecha", item.fecha())
                        .addValue("transaccion", item.transaccion())
                        .addValue("monto", item.monto())
                        .addValue("descripcion", item.descripcion()))
                .build();
    }

    @Bean
    Step transaccionesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<LegacyTransaccion> transaccionesReader,
            TransaccionProcessor transaccionProcessor,
            JdbcBatchItemWriter<TransaccionProcesada> transaccionesWriter,
            JdbcTemplate jdbcTemplate,
            @Value("${banco.batch.chunk-size}") int chunkSize
    ) {
        return new StepBuilder("transaccionesStep", jobRepository)
                .<LegacyTransaccion, TransaccionProcesada>chunk(chunkSize, transactionManager)
                .reader(transaccionesReader)
                .processor(transaccionProcessor)
                .writer(transaccionesWriter)
                .faultTolerant()
                .retry(TransientDataAccessException.class)
                .retryLimit(3)
                .skip(IllegalArgumentException.class)
                .skipLimit(2000)
                .listener(new RegistroSkipListener<LegacyTransaccion, TransaccionProcesada>(
                        "transaccionesJob", jdbcTemplate))
                .build();
    }

    @Bean
    Step interesesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<LegacyInteres> interesesReader,
            InteresProcessor interesProcessor,
            JdbcBatchItemWriter<InteresCalculado> interesesWriter,
            JdbcTemplate jdbcTemplate,
            @Value("${banco.batch.chunk-size}") int chunkSize
    ) {
        return new StepBuilder("interesesStep", jobRepository)
                .<LegacyInteres, InteresCalculado>chunk(chunkSize, transactionManager)
                .reader(interesesReader)
                .processor(interesProcessor)
                .writer(interesesWriter)
                .faultTolerant()
                .retry(TransientDataAccessException.class)
                .retryLimit(3)
                .skip(IllegalArgumentException.class)
                .skipLimit(2000)
                .listener(new RegistroSkipListener<LegacyInteres, InteresCalculado>(
                        "interesesJob", jdbcTemplate))
                .build();
    }

    @Bean
    Step cuentasAnualesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<LegacyMovimiento> cuentasAnualesReader,
            MovimientoAnualProcessor movimientoAnualProcessor,
            JdbcBatchItemWriter<MovimientoAnual> cuentasAnualesWriter,
            JdbcTemplate jdbcTemplate,
            @Value("${banco.batch.chunk-size}") int chunkSize
    ) {
        return new StepBuilder("cuentasAnualesStep", jobRepository)
                .<LegacyMovimiento, MovimientoAnual>chunk(chunkSize, transactionManager)
                .reader(cuentasAnualesReader)
                .processor(movimientoAnualProcessor)
                .writer(cuentasAnualesWriter)
                .faultTolerant()
                .retry(TransientDataAccessException.class)
                .retryLimit(3)
                .skip(IllegalArgumentException.class)
                .skipLimit(2000)
                .listener(new RegistroSkipListener<LegacyMovimiento, MovimientoAnual>(
                        "cuentasAnualesJob", jdbcTemplate))
                .build();
    }

    @Bean
    Step resumenAnualStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            JdbcTemplate jdbcTemplate
    ) {
        return new StepBuilder("resumenAnualStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    jdbcTemplate.update("""
                            INSERT INTO batch_estados_cuenta_anuales
                                (cuenta_id, anio, cantidad_movimientos, total_depositos,
                                 total_cargos, saldo_neto, generado_en)
                            SELECT cuenta_id,
                                   EXTRACT(YEAR FROM fecha)::INTEGER,
                                   COUNT(*),
                                   COALESCE(SUM(CASE WHEN transaccion = 'DEPOSITO' THEN monto ELSE 0 END), 0),
                                   COALESCE(SUM(CASE WHEN transaccion IN ('RETIRO', 'COMPRA') THEN monto ELSE 0 END), 0),
                                   COALESCE(SUM(CASE WHEN transaccion = 'DEPOSITO' THEN monto ELSE -monto END), 0),
                                   CURRENT_TIMESTAMP
                            FROM batch_movimientos_anuales
                            GROUP BY cuenta_id, EXTRACT(YEAR FROM fecha)
                            ON CONFLICT (cuenta_id, anio) DO UPDATE SET
                                cantidad_movimientos = EXCLUDED.cantidad_movimientos,
                                total_depositos = EXCLUDED.total_depositos,
                                total_cargos = EXCLUDED.total_cargos,
                                saldo_neto = EXCLUDED.saldo_neto,
                                generado_en = CURRENT_TIMESTAMP
                            """);
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    Job transaccionesJob(
            JobRepository jobRepository,
            @Qualifier("transaccionesStep") Step step
    ) {
        return new JobBuilder("transaccionesJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(new BatchMetricsListener())
                .start(step)
                .build();
    }

    @Bean
    Job interesesJob(
            JobRepository jobRepository,
            @Qualifier("interesesStep") Step step
    ) {
        return new JobBuilder("interesesJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(new BatchMetricsListener())
                .start(step)
                .build();
    }

    @Bean
    Job cuentasAnualesJob(
            JobRepository jobRepository,
            @Qualifier("cuentasAnualesStep") Step cuentasAnualesStep,
            @Qualifier("resumenAnualStep") Step resumenAnualStep
    ) {
        return new JobBuilder("cuentasAnualesJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(new BatchMetricsListener())
                .start(cuentasAnualesStep)
                .next(resumenAnualStep)
                .build();
    }
}
