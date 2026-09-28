package com.pedidos360.auditoria.auditoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Un evento visto pasar por el topic. Guarda el sobre (qué routing key
 * traía) y el cuerpo crudo: la bitácora no necesita entender cada evento.
 */
@Entity
@Table(name = "eventos")
public class EventoAuditoria {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String routingKey;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String cuerpo;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  protected EventoAuditoria() {}

  public EventoAuditoria(String routingKey, String cuerpo) {
    this.routingKey = routingKey;
    this.cuerpo = cuerpo;
  }

  public Long getId() { return id; }
  public String getRoutingKey() { return routingKey; }
  public String getCuerpo() { return cuerpo; }
  public Instant getCreatedAt() { return createdAt; }
}
