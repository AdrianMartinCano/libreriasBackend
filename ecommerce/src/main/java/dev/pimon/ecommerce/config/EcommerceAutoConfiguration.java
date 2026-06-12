package dev.pimon.ecommerce.config;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

@AutoConfiguration
@EnableConfigurationProperties(EcommerceProperties.class)
@ComponentScan(basePackages = "dev.pimon.ecommerce")
@Import(EcommerceAutoConfiguration.EcommercePackageRegistrar.class)
public class EcommerceAutoConfiguration {

    static class EcommercePackageRegistrar implements ImportBeanDefinitionRegistrar {
        @Override
        public void registerBeanDefinitions(AnnotationMetadata metadata,
                                            BeanDefinitionRegistry registry) {
            AutoConfigurationPackages.register(registry, "dev.pimon.ecommerce");
        }
    }
}
