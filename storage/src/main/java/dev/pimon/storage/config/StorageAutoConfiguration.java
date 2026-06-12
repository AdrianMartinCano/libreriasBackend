package dev.pimon.storage.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import dev.pimon.storage.service.WebPConverter;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
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
}
