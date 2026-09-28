package com.pedidos360.auditoria.auditoria;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, Long> {

  List<EventoAuditoria> findTop50ByOrderByCreatedAtDesc();

  List<EventoAuditoria> findByCuerpoContainingOrderByCreatedAtDesc(String texto);
}
