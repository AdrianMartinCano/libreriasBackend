package dev.pimon.security.config;

import dev.pimon.security.resolver.CurrentUserArgumentResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Autoconfigura libui-security al incluirlo como dependencia.
 * Registra el CurrentUserArgumentResolver para @CurrentUser en controllers.
 */
@AutoConfiguration
@EnableConfigurationProperties(SecurityProperties.class)
@ComponentScan(basePackages = "dev.pimon.security")
public class SecurityAutoConfiguration implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserResolver;

    public SecurityAutoConfiguration(CurrentUserArgumentResolver currentUserResolver) {
        this.currentUserResolver = currentUserResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserResolver);
    }
}
