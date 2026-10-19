package dev.pimon.ecommerce.config;

import dev.pimon.ecommerce.service.CalculadorEnvio;
import dev.pimon.ecommerce.service.EnvioTarifaPlana;
import dev.pimon.ecommerce.service.ValidadorOpciones;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

@AutoConfiguration
@EnableConfigurationProperties(EcommerceProperties.class)
@ComponentScan(basePackages = "dev.pimon.ecommerce")
@Import(EcommerceAutoConfiguration.EcommercePackageRegistrar.class)
public class EcommerceAutoConfiguration {

    /**
     * El cálculo del envío por defecto: tarifa plana, como siempre.
     *
     * {@code @ConditionalOnMissingBean} es lo que hace que este cambio no rompa nada: si
     * el proyecto declara su propio {@link CalculadorEnvio} —por ejemplo uno que cobre
     * por peso volumétrico— gana el suyo y este no llega a crearse. Y si no declara
     * ninguno, todo sigue funcionando exactamente igual que antes.
     */
    @Bean
    @ConditionalOnMissingBean(CalculadorEnvio.class)
    public CalculadorEnvio calculadorEnvio(EcommerceProperties props) {
        return new EnvioTarifaPlana(props);
    }

    /**
     * Por defecto, las opciones de línea se aceptan tal cual.
     *
     * Mismo motivo que el calculador de envío: una tienda que no use opciones no tiene que
     * implementar nada, y si el proyecto declara su propio {@link ValidadorOpciones} —por
     * ejemplo pimon3d, que comprueba que el color exista y esté disponible— gana el suyo.
     *
     * NO VALIDAR NO ES UN AGUJERO aquí: si una tienda no manda opciones, este validador no
     * ve nada; y si las manda sin declarar reglas, es que le vale cualquier valor. Lo que
     * sí protege siempre es el precio, que nunca viene del cliente.
     */
    @Bean
    @ConditionalOnMissingBean(ValidadorOpciones.class)
    public ValidadorOpciones validadorOpciones() {
        return (producto, opciones) -> { };
    }

    static class EcommercePackageRegistrar implements ImportBeanDefinitionRegistrar {
        @Override
        public void registerBeanDefinitions(AnnotationMetadata metadata,
                                            BeanDefinitionRegistry registry) {
            AutoConfigurationPackages.register(registry, "dev.pimon.ecommerce");
        }
    }
}
