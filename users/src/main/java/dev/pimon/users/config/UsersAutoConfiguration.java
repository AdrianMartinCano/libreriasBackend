package dev.pimon.users.config;

import lombok.RequiredArgsConstructor;
import dev.pimon.users.service.UserService;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Autoconfigura libui-users al incluirlo como dependencia.
 *
 * Usa AutoConfigurationPackages.register() para AÑADIR el paquete de
 * usuarios al escaneo de Spring Boot sin sobreescribir el de la app.
 * Así NotaRepository (dev.pimon.demo) y UserRepository (dev.pimon.users)
 * coexisten sin problemas.
 */
@AutoConfiguration
@EnableConfigurationProperties(UsersProperties.class)
@ComponentScan(basePackages = "dev.pimon.users")
@Import(UsersAutoConfiguration.UsersPackageRegistrar.class)
@RequiredArgsConstructor
public class UsersAutoConfiguration {

    /**
     * Registra dev.pimon.users en AutoConfigurationPackages.
     * Spring Boot usa esta lista para escanear entidades y repositorios JPA.
     * No sobreescribe el paquete principal de la aplicación.
     */
    static class UsersPackageRegistrar implements ImportBeanDefinitionRegistrar {
        @Override
        public void registerBeanDefinitions(AnnotationMetadata metadata,
                                            BeanDefinitionRegistry registry) {
            AutoConfigurationPackages.register(registry, "dev.pimon.users");
        }
    }

    @Bean
    ApplicationRunner initDefaultAdmin(UserService userService, UsersProperties props) {
        return args -> {
            if (props.isCreateDefaultAdmin()) {
                userService.createDefaultAdmin(
                        props.getAdminEmail(),
                        props.getAdminPassword(),
                        props.getAdminName()
                );
            }
        };
    }
}
