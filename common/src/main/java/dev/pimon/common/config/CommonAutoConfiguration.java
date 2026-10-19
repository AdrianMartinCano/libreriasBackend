package dev.pimon.common.config;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Autoconfigura la librería common al incluirla como dependencia.
 * Activa el escaneo de componentes (GlobalExceptionHandler, SettingsStore, etc.),
 * el auditing JPA (createdAt, updatedAt en BaseEntity) y registra el paquete
 * dev.pimon.common para que se escaneen su entidad (app_settings) y su repositorio.
 */
@AutoConfiguration
@ComponentScan(basePackages = "dev.pimon.common")
@EnableJpaAuditing
@Import(CommonAutoConfiguration.CommonPackageRegistrar.class)
public class CommonAutoConfiguration {

    /** Añade dev.pimon.common al escaneo de entidades/repositorios JPA sin pisar el de la app. */
    static class CommonPackageRegistrar implements ImportBeanDefinitionRegistrar {
        @Override
        public void registerBeanDefinitions(AnnotationMetadata metadata,
                                            BeanDefinitionRegistry registry) {
            AutoConfigurationPackages.register(registry, "dev.pimon.common");
        }
    }
}
