package com.pedidos360.auditoria.controller;

import com.pedidos360.auditoria.auditoria.EventoAuditoria;
import com.pedidos360.auditoria.auditoria.EventoAuditoriaRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lectura de la bitácora. Sin Spring Security a propósito: en AWS lo
 * protege el authorizer JWT del API Gateway (igual que los demás nuevos).
 */
@RestController
@RequestMapping("/auditoria")
public class AuditoriaController {

  private final EventoAuditoriaRepository repository;

  public AuditoriaController(EventoAuditoriaRepository repository) {
    this.repository = repository;
  }

  @GetMapping
  public List<Map<String, Object>> eventos(
      @RequestParam(required = false) String ordenId) {
    List<EventoAuditoria> eventos = ordenId == null || ordenId.isBlank()
        ? repository.findTop50ByOrderByCreatedAtDesc()
        : repository.findByCuerpoContainingOrderByCreatedAtDesc(ordenId);
    return eventos.stream().map(e -> {
          // LinkedHashMap (no Map.of): el id puede venir null en respuestas
          // de prueba y Map.of revienta con NPE ante cualquier null.
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("id", e.getId());
          m.put("routingKey", e.getRoutingKey());
          m.put("cuerpo", e.getCuerpo());
          m.put("createdAt", e.getCreatedAt() == null ? null : e.getCreatedAt().toString());
          return m;
        })
        .collect(Collectors.toList());
  }
}
