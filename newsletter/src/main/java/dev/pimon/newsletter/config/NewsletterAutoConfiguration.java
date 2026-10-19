package dev.pimon.newsletter.config;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@AutoConfiguration
@EnableAsync
@ComponentScan(basePackages = "dev.pimon.newsletter")
@EnableConfigurationProperties(NewsletterProperties.class)
@Import(NewsletterAutoConfiguration.NewsletterPackageRegistrar.class)
public class NewsletterAutoConfiguration {

    /**
     * Ejecutor ACOTADO para el envío de campañas (lo usa {@code CampaignDispatcher}). Pocos hilos y
     * cola limitada para no saturar el SMTP ni la memoria con listas muy grandes.
     */
    @Bean("newsletterExecutor")
    TaskExecutor newsletterExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("newsletter-");
        executor.initialize();
        return executor;
    }

    static class NewsletterPackageRegistrar implements ImportBeanDefinitionRegistrar {
        @Override
        public void registerBeanDefinitions(AnnotationMetadata metadata,
                                            BeanDefinitionRegistry registry) {
            AutoConfigurationPackages.register(registry, "dev.pimon.newsletter");
        }
    }
}
