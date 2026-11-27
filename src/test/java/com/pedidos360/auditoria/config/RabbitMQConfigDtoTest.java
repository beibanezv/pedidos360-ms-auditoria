package com.pedidos360.auditoria.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.MessageConverter;

/** M06b: la bitacora consume Message crudo, pero debe resolver cualquier TypeId. */
class RabbitMQConfigDtoTest {

  private static final String TYPE_ID = DefaultJackson2JavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME;
  private final MessageConverter converter = new RabbitMQConfig().jsonMessageConverter();

  @Test
  void mapperUsaTypeIdYMapeaNombreSimpleYFqcnHistoricoAObject() {
    DefaultJackson2JavaTypeMapper mapper = RabbitMQConfig.jsonTypeMapper();

    assertThat(mapper.getTypePrecedence()).isEqualTo(Jackson2JavaTypeMapper.TypePrecedence.TYPE_ID);
    assertThat(mapper.getIdClassMapping())
        .containsEntry("OrdenRegistradaEvent", Object.class)
        .containsEntry("OrdenCreadaEvent", Object.class)
        .containsEntry("StockReservadoEvent", Object.class)
        .containsEntry("StockRechazadoEvent", Object.class)
        .containsEntry("StockLiberadoEvent", Object.class)
        .containsEntry("PagoResueltoEvent", Object.class)
        .containsEntry("com.pedidos360.orders.messaging.OrdenRegistradaEvent", Object.class)
        .doesNotContainKey("*");
  }

  @Test
  void consumirConNombreSimpleConservaTodosLosCamposDelCuerpo() {
    verificar("OrdenRegistradaEvent");
  }

  @Test
  void consumirConFqcnHistoricoConservaTodosLosCamposDelCuerpo() {
    verificar("com.pedidos360.orders.messaging.OrdenRegistradaEvent");
  }

  private void verificar(String typeId) {
    String orden = "11111111-1111-1111-1111-111111111111";
    String producto = "22222222-2222-2222-2222-222222222222";
    Map<String, Object> cuerpo = Map.of(
        "ordenId", orden,
        "usuarioId", "u-1",
        "emailComprador", "comprador@example.cl",
        "items", List.of(Map.of("productoId", producto, "cantidad", 2)),
        "codigoCupon", "CUPON10");

    // Converter real de auditoria; se timbra el nombre simple como hace el productor.
    Message mensaje = converter.toMessage(cuerpo, new MessageProperties());
    mensaje.getMessageProperties().setHeader(TYPE_ID, typeId);
    assertThat((Object) mensaje.getMessageProperties().getHeader(TYPE_ID)).isEqualTo(typeId);

    Object resultado = converter.fromMessage(mensaje);
    assertThat(resultado).isInstanceOf(Map.class);
    @SuppressWarnings("unchecked")
    Map<String, Object> leido = (Map<String, Object>) resultado;
    assertThat(leido)
        .containsEntry("ordenId", orden)
        .containsEntry("usuarioId", "u-1")
        .containsEntry("emailComprador", "comprador@example.cl")
        .containsEntry("codigoCupon", "CUPON10");
    assertThat(leido.get("items")).asList().hasSize(1);
    @SuppressWarnings("unchecked")
    Map<String, Object> item = (Map<String, Object>) ((List<?>) leido.get("items")).get(0);
    assertThat(item).containsEntry("productoId", producto).containsEntry("cantidad", 2);
  }
}
