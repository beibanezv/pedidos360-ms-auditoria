package com.pedidos360.auditoria.messaging;

import com.pedidos360.auditoria.auditoria.EventoAuditoria;
import com.pedidos360.auditoria.auditoria.EventoAuditoriaRepository;
import com.pedidos360.auditoria.config.RabbitMQConfig;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consumidor del dominio "auditoría". Recibe el mensaje crudo (sin
 * convertir a ningún evento concreto) y guarda sobre + cuerpo.
 *
 * Decisiones de error (indicador 5):
 * - Cuerpo vacío → PERMANENTE → DLQ directo (no hay nada que guardar).
 * - Fallo de BD → TRANSITORIO → requeue hasta MAX_REINTENTOS, después DLQ.
 */
@Component
public class AuditoriaConsumer extends BaseConsumer {

  private final EventoAuditoriaRepository repository;

  public AuditoriaConsumer(EventoAuditoriaRepository repository) {
    this.repository = repository;
  }

  @Override
  protected int maxReintentos() {
    return RabbitMQConfig.MAX_REINTENTOS;
  }

  @RabbitListener(queues = RabbitMQConfig.AUDITORIA_QUEUE)
  public void auditar(Message raw, Channel channel,
      @Header(AmqpHeaders.DELIVERY_TAG) long tag,
      @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) throws IOException {
    consumir(channel, tag, raw, () -> {
      String cuerpo = new String(raw.getBody(), StandardCharsets.UTF_8);
      if (cuerpo.isBlank()) {
        throw new IllegalArgumentException("evento con cuerpo vacío");
      }
      repository.save(new EventoAuditoria(routingKey, cuerpo));
    });
  }
}
