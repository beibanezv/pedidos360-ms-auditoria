package com.pedidos360.auditoria.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.LinkedHashMap;
import java.util.Map;
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

  // Este consumer recibe Message crudo, pero el converter igual resuelve el
  // __TypeId__ que timbra ms-orders con SU clase (que aquí no existe): sin
  // este mapeo, Class.forName del id foráneo mandaba todo evento real a la
  // DLQ. Se mapea a Object (solo importa no reventar; el payload se lee crudo).
  // Método separado para poder testearlo sin levantar contexto Spring.
  public static DefaultJackson2JavaTypeMapper jsonTypeMapper() {
    Map<String, Class<?>> ids = new LinkedHashMap<>();

    // Alias FQCN historicos de todos los productores -> Object (cuerpo crudo).
    ids.put("com.pedidos360.carrito.messaging.OrdenCreadaEvent", Object.class);
    ids.put("com.pedidos360.orders.messaging.OrdenCreadaEvent", Object.class);
    ids.put("com.pedidos360.orders.messaging.OrdenRegistradaEvent", Object.class);
    ids.put("com.pedidos360.productos.messaging.OrdenRegistradaEvent", Object.class);
    ids.put("com.pedidos360.notificaciones.messaging.OrdenRegistradaEvent", Object.class);
    ids.put("cl.pedidos360.cupones.messaging.OrdenRegistradaEvent", Object.class);
    ids.put("com.pedidos360.productos.messaging.StockReservadoEvent", Object.class);
    ids.put("com.pedidos360.orders.messaging.StockReservadoEvent", Object.class);
    ids.put("com.pedidos360.productos.messaging.StockRechazadoEvent", Object.class);
    ids.put("com.pedidos360.orders.messaging.StockRechazadoEvent", Object.class);
    ids.put("com.pedidos360.productos.messaging.StockLiberadoEvent", Object.class);
    ids.put("com.pedidos360.orders.messaging.StockLiberadoEvent", Object.class);
    ids.put("com.pedidos360.pagos.PagoResueltoEvent", Object.class);
    ids.put("com.pedidos360.pagos.messaging.PagoResueltoEvent", Object.class);
    ids.put("com.pedidos360.orders.messaging.PagoResueltoEvent", Object.class);

    // Nombres simples al final: son los que el productor escribe en __TypeId__.
    ids.put("OrdenCreadaEvent", Object.class);
    ids.put("OrdenRegistradaEvent", Object.class);
    ids.put("StockReservadoEvent", Object.class);
    ids.put("StockRechazadoEvent", Object.class);
    ids.put("StockLiberadoEvent", Object.class);
    ids.put("PagoResueltoEvent", Object.class);

    DefaultJackson2JavaTypeMapper mapper = new DefaultJackson2JavaTypeMapper();
    mapper.setIdClassMapping(ids);
    mapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.TYPE_ID);
    return mapper;
  }

  /** Nombre anterior conservado por compatibilidad con tests existentes. */
  public static DefaultJackson2JavaTypeMapper ordenRegistradaTypeMapper() {
    return jsonTypeMapper();
  }

  @Bean
  MessageConverter jsonMessageConverter() {
    Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
    converter.setJavaTypeMapper(jsonTypeMapper());
    return converter;
  }
}
