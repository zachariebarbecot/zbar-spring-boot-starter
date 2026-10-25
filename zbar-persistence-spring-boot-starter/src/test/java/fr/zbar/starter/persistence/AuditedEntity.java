package fr.zbar.starter.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
class AuditedEntity {

  @Id @GeneratedValue Long id;

  Instant createdAt;
}
