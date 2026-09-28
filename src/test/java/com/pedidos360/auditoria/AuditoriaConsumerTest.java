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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

@ExtendWith(MockitoExtension.class)
class AuditoriaConsumerTest {

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

    verify(channel).basicNack(TAG, false, true);
  }
}
