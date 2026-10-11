package com.duoc.transaccionesservice.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PagoEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public PagoEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${banco.kafka.topic.pagos}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publicar(PagoEvent evento) {
        kafkaTemplate.send(topic, evento.operacionId(), evento);
    }
}
