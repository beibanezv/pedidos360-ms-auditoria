package com.pedidos360.auditoria;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pedidos360.auditoria.auditoria.EventoAuditoria;
import com.pedidos360.auditoria.auditoria.EventoAuditoriaRepository;
import com.pedidos360.auditoria.controller.AuditoriaController;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuditoriaController.class)
class AuditoriaControllerTest {

  @Autowired
  MockMvc mvc;

  @MockBean
  EventoAuditoriaRepository repository;

  @Test
  void sinFiltro_retornaUltimos() throws Exception {
    when(repository.findTop50ByOrderByCreatedAtDesc())
        .thenReturn(List.of(new EventoAuditoria("orden.registrada", "{}")));

    mvc.perform(get("/auditoria"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].routingKey").value("orden.registrada"));
  }

  @Test
  void conOrdenId_filtraPorCuerpo() throws Exception {
    when(repository.findByCuerpoContainingOrderByCreatedAtDesc("abc"))
        .thenReturn(List.of(new EventoAuditoria("orden.registrada", "{\"ordenId\":\"abc\"}")));

    mvc.perform(get("/auditoria").param("ordenId", "abc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].cuerpo").value("{\"ordenId\":\"abc\"}"));
  }
}
