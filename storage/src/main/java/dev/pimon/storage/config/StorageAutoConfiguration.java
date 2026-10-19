package dev.pimon.storage.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import dev.pimon.storage.service.WebPConverter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(StorageProperties.class)
@ComponentScan(basePackages = "dev.pimon.storage")
@RequiredArgsConstructor
public class StorageAutoConfiguration {

    private final StorageProperties props;

    @Bean
    ApplicationRunner storageInit(WebPConverter converter) {
        return args -> {
            Path uploadDir = Path.of(props.getUploadDir());
            try {
                Files.createDirectories(uploadDir);
                log.info("Storage: directorio base = {}", uploadDir.toAbsolutePath());
            } catch (IOException e) {
                log.error("No se pudo crear el directorio de uploads: {}", e.getMessage());
            }

            if (props.getWebp().isEnabled()) {
                converter.isAvailable();
            }

            log.info("Storage: base-url = {}", props.getBaseUrl());
            log.info("Storage: WebP conversion = {}", props.getWebp().isEnabled());
            log.info("Storage: max-size = {} MB", props.getMaxSizeMb());
        };
    }

    @Bean
    WebMvcConfigurer storageResourceHandler() {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                Path uploadDir = Path.of(props.getUploadDir()).toAbsolutePath();
                registry.addResourceHandler("/uploads/**")
                        .addResourceLocations("file:" + uploadDir + "/");
                log.info("Storage: sirviendo /uploads/** desde {}", uploadDir);
            }
        };
    }

    /**
     * Cabeceras de seguridad para los archivos servidos en /uploads:
     * <ul>
     *   <li>{@code X-Content-Type-Options: nosniff} — impide que el navegador "adivine" el tipo
     *       (defensa contra MIME sniffing).</li>
     *   <li>Los {@code .svg} se sirven con {@code Content-Disposition: attachment} para que NUNCA
     *       se rendericen inline (mitiga XSS si alguien reactiva el tipo SVG).</li>
     * </ul>
     */
    @Bean
    FilterRegistrationBean<OncePerRequestFilter> uploadsSecurityHeadersFilter() {
        OncePerRequestFilter filter = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain) throws ServletException, IOException {
                response.setHeader("X-Content-Type-Options", "nosniff");
                if (request.getRequestURI().toLowerCase().endsWith(".svg")) {
                    response.setHeader("Content-Disposition", "attachment");
                }
                chain.doFilter(request, response);
            }
        };
        FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/uploads/*");
        registration.setName("uploadsSecurityHeadersFilter");
        return registration;
    }
}
