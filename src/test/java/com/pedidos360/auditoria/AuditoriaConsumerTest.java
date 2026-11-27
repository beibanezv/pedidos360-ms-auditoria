package com.pedidos360.auditoria;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pedidos360.auditoria.auditoria.EventoAuditoria;
import com.pedidos360.auditoria.auditoria.EventoAuditoriaRepository;
import com.pedidos360.auditoria.messaging.AuditoriaConsumer;
import com.rabbitmq.client.Channel;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

@ExtendWith(MockitoExtension.class)
class AuditoriaConsumerTest {
  static { System.setProperty("p360.retry.espera-ms", "0"); }

  @Mock
  EventoAuditoriaRepository repository;
  @Mock
  Channel channel;

  @InjectMocks
  AuditoriaConsumer consumer;

  private static final long TAG = 7L;

  private static Message mensaje(String cuerpo) {
    return new Message(cuerpo.getBytes(StandardCharsets.UTF_8), new MessageProperties());
  }

  @Test
  void valido_guardaYack() throws Exception {
    consumer.auditar(mensaje("{\"ordenId\":\"1\"}"), channel, TAG, "orden.registrada");

    verify(repository).save(any(EventoAuditoria.class));
    verify(channel).basicAck(TAG, false);
  }

  @Test
  void vacio_dlqDirecto() throws Exception {
    consumer.auditar(mensaje("   "), channel, TAG, "orden.registrada");

    verify(repository, never()).save(any(EventoAuditoria.class));
    verify(channel).basicNack(TAG, false, false);
  }

  @Test
  void bdCaida_reintenta() throws Exception {
    when(repository.save(any(EventoAuditoria.class)))
        .thenThrow(new RuntimeException("BD caída"));

    consumer.auditar(mensaje("{\"ordenId\":\"1\"}"), channel, TAG, "orden.registrada");

    verify(channel).basicNack(TAG, false, false);
  }

  // Regresión Fase 6 (AWS): ms-orders timbra __TypeId__ con SU clase
  // (com.pedidos360.orders.messaging.OrdenRegistradaEvent), que no existe
  // aquí. Sin el mapeo del bean, todo evento real caía a la DLQ con
  // ClassNotFoundException aunque este consumer solo lee el Message crudo.
  @Test
  void eventoRealDelBroker_noRevientaPorTypeIdForaneo() {
    UUID orden = UUID.randomUUID();
    UUID prod = UUID.randomUUID();
    MessageProperties props = new MessageProperties();
    props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
    props.setHeader(DefaultJackson2JavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME,
        "com.pedidos360.orders.messaging.OrdenRegistradaEvent");
    Message msg = new Message(
        ("{\"ordenId\":\"" + orden + "\",\"usuarioId\":\"u-1\",\"items\":"
            + "[{\"productoId\":\"" + prod + "\",\"cantidad\":1}],\"codigoCupon\":null}")
            .getBytes(StandardCharsets.UTF_8),
        props);

    Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
    converter.setJavaTypeMapper(
        com.pedidos360.auditoria.config.RabbitMQConfig.ordenRegistradaTypeMapper());

    org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> converter.fromMessage(msg));
  }
}
