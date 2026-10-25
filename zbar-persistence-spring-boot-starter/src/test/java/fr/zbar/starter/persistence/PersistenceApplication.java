package fr.zbar.starter.persistence;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackageClasses = AuditedEntity.class)
class PersistenceApplication {}
