package com.pedidos360.auditoria.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología RabbitMQ que usa ms-auditoria (rúbrica EP3/EP4 indicadores 1 y 2).
 *
 * Ruta documentada:
 *   ms-orders --topic--> p360.eventos / #(todo)
 *                            └─> p360.auditoria.queue (la consume AuditoriaConsumer)
 *   El binding "#" recibe copia de cada evento del topic sin importar su
 *   routing key: si mañana se agrega otro evento, la bitácora lo guarda
 *   sin cambiar código. Si el mensaje muere va a p360.dlx y de ahí a su DLQ.
 */
@Configuration
public class RabbitMQConfig {

  public static final String EVENTOS_EXCHANGE = "p360.eventos";
  public static final String AUDITORIA_QUEUE = "p360.auditoria.queue";
  /** Patrón topic: todo lo que pase por el exchange. */
  public static final String TODOS_LOS_EVENTOS = "#";

  public static final int MAX_REINTENTOS = 3;

  public static final String DLX_EXCHANGE = "p360.dlx";
  public static final String AUDITORIA_DLQ = "p360.auditoria.dlq";

  @Bean
  TopicExchange eventosExchange() {
    return new TopicExchange(EVENTOS_EXCHANGE, true, false);
  }

  @Bean
  Queue auditoriaQueue() {
    return QueueBuilder.durable(AUDITORIA_QUEUE)
        .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
        .withArgument("x-dead-letter-routing-key", AUDITORIA_QUEUE)
        .build();
  }

  @Bean
  Binding auditoriaBinding(Queue auditoriaQueue, TopicExchange eventosExchange) {
    return new Binding(AUDITORIA_QUEUE, Binding.DestinationType.QUEUE,
        EVENTOS_EXCHANGE, TODOS_LOS_EVENTOS, null);
  }

  @Bean
  DirectExchange deadLetterExchange() {
    return new DirectExchange(DLX_EXCHANGE, true, false);
  }

  @Bean
  Queue auditoriaDlq() {
    return QueueBuilder.durable(AUDITORIA_DLQ).build();
  }

  @Bean
  Binding auditoriaDlqBinding(Queue auditoriaDlq, DirectExchange deadLetterExchange) {
    return new Binding(AUDITORIA_DLQ, Binding.DestinationType.QUEUE,
        DLX_EXCHANGE, AUDITORIA_QUEUE, null);
  }

  @Bean
  MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }
}
