package dev.pimon.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Autoconfigura la librería common al incluirla como dependencia.
 * Activa el escaneo de componentes (GlobalExceptionHandler, etc.)
 * y el auditing JPA (createdAt, updatedAt en BaseEntity).
 */
@AutoConfiguration
@ComponentScan(basePackages = "dev.pimon.common")
@EnableJpaAuditing
public class CommonAutoConfiguration {
}
