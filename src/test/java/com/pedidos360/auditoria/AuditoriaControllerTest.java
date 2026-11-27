package com.pedidos360.auditoria;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pedidos360.auditoria.auditoria.EventoAuditoria;
import com.pedidos360.auditoria.auditoria.EventoAuditoriaRepository;
import com.pedidos360.auditoria.config.SecurityConfig;
import com.pedidos360.auditoria.controller.AuditoriaController;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(AuditoriaController.class)
@Import(SecurityConfig.class)
class AuditoriaControllerTest {

  @Autowired
  MockMvc mvc;

  @MockBean
  EventoAuditoriaRepository repository;

  @MockBean
  JwtDecoder jwtDecoder;

  private static MockHttpServletRequestBuilder conAdmin(MockHttpServletRequestBuilder peticion) {
    return peticion.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin")));
  }

  @Test
  void sinFiltro_retornaUltimos() throws Exception {
    when(repository.findTop50ByOrderByCreatedAtDesc())
        .thenReturn(List.of(new EventoAuditoria("orden.registrada", "{}")));

    mvc.perform(conAdmin(get("/auditoria")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].routingKey").value("orden.registrada"));
  }

  @Test
  void conOrdenId_filtraPorCuerpo() throws Exception {
    when(repository.findByCuerpoContainingOrderByCreatedAtDesc("abc"))
        .thenReturn(List.of(new EventoAuditoria("orden.registrada", "{\"ordenId\":\"abc\"}")));

    mvc.perform(conAdmin(get("/auditoria").param("ordenId", "abc")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].cuerpo").value("{\"ordenId\":\"abc\"}"));
  }

  @Test
  void sinJwt_retorna401() throws Exception {
    mvc.perform(get("/auditoria"))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(repository);
  }

  @Test
  void clienteAzure_retorna403() throws Exception {
    mvc.perform(get("/auditoria")
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Cliente"))))
        .andExpect(status().isForbidden());

    verifyNoInteractions(repository);
  }

  @Test
  void jwtInvalido_retorna401() throws Exception {
    when(jwtDecoder.decode("invalido")).thenThrow(new BadJwtException("token inválido"));

    mvc.perform(get("/auditoria").header(HttpHeaders.AUTHORIZATION, "Bearer invalido"))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(repository);
  }

  @Test
  void preflight_origenPermitido_retornaOkConOrigenExacto() throws Exception {
    mvc.perform(options("/auditoria")
            .header(HttpHeaders.ORIGIN, "http://localhost:4200")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));

    verifyNoInteractions(repository);
  }

  @Test
  void preflight_origenAjeno_retorna403SinOrigen() throws Exception {
    mvc.perform(options("/auditoria")
            .header(HttpHeaders.ORIGIN, "http://atacante.example")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));

    verifyNoInteractions(repository);
  }
}
