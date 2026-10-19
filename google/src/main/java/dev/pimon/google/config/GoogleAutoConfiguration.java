package dev.pimon.google.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import java.util.List;

/**
 * Autoconfigura el login con Google. <b>Solo si {@code pimon.google.habilitado=true}.</b>
 *
 * <h2>Por qué todo el módulo va condicionado, y no solo el endpoint</h2>
 *
 * Porque aquí hay una <b>entidad JPA</b>. Registrar el paquete sin más obligaría a tener la
 * tabla {@code cuentas_google} a cualquiera que incluyese el artefacto, y una aplicación con
 * {@code ddl-auto: validate} —pimon3d, sin ir más lejos— no arrancaría hasta escribir una
 * migración de algo que no usa. Con la condición, quien no lo enciende no carga nada: ni
 * tabla, ni endpoints, ni conexión a Google.
 *
 * <p>Efecto secundario a tener en cuenta desde el front: con el módulo apagado,
 * {@code GET /api/auth/google/config} <b>no existe</b> y responde 404. Eso significa «aquí no
 * hay login con Google», y es lo que el botón tiene que interpretar para no pintarse.
 *
 * <h2>Hace falta la migración de la tabla</h2>
 *
 * Las librerías traen la entidad, <b>no</b> las migraciones: las escribe cada proyecto, que
 * es quien sabe con qué versión va su esquema. Ver el README del módulo.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "pimon.google", name = "habilitado", havingValue = "true")
@EnableConfigurationProperties(GoogleProperties.class)
@ComponentScan(basePackages = "dev.pimon.google")
@Import(GoogleAutoConfiguration.GooglePackageRegistrar.class)
public class GoogleAutoConfiguration {

    /** Donde Google publica las claves públicas con las que firma los id_token. */
    private static final String CLAVES_DE_GOOGLE = "https://www.googleapis.com/oauth2/v3/certs";

    /**
     * Añade {@code dev.pimon.google} al escaneo de entidades y repositorios JPA, sin pisar el
     * paquete de la aplicación ni el de las demás librerías.
     */
    static class GooglePackageRegistrar implements ImportBeanDefinitionRegistrar {
        @Override
        public void registerBeanDefinitions(AnnotationMetadata metadata,
                                            BeanDefinitionRegistry registry) {
            AutoConfigurationPackages.register(registry, "dev.pimon.google");
        }
    }

    /**
     * El decodificador de los {@code id_token} de Google.
     *
     * <p>Se construye con la URL de las claves ({@code withJwkSetUri}) y <b>no</b> con
     * {@code JwtDecoders.fromIssuerLocation}, aunque esa segunda sea más corta: la segunda se
     * conecta a Google <b>al crear el bean</b>, o sea, al arrancar la aplicación. Si Google no
     * contestara en ese momento —o el arranque fuera sin red, como en los tests— la aplicación
     * entera no levantaría por una función que es secundaria.
     *
     * <p>Con {@code withJwkSetUri} la primera conexión ocurre en el primer login, y Nimbus se
     * encarga a partir de ahí de cachear las claves y volver a pedirlas cuando Google las rota
     * (lo hace cada pocas semanas).
     *
     * @return un decodificador que ya valida firma, caducidad y emisor. Lo que le falta
     *         —{@code aud} y {@code email_verified}— lo comprueba el verificador.
     */
    @Bean
    public JwtDecoder decodificadorDeGoogle(GoogleProperties props) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withJwkSetUri(CLAVES_DE_GOOGLE).build();
        decodificador.setJwtValidator(new ValidadorDeEmisores(props.emisores()));
        return decodificador;
    }

    /**
     * Valida la caducidad (lo estándar) y que el emisor sea uno de los de Google.
     *
     * Google pone en el {@code iss} unas veces {@code https://accounts.google.com} y otras
     * {@code accounts.google.com}, según el flujo. Las dos son suyas y las dos valen, así que
     * el validador de un solo emisor que trae Spring no sirve tal cual.
     */
    record ValidadorDeEmisores(List<String> emisores) implements OAuth2TokenValidator<Jwt> {

        @Override
        public OAuth2TokenValidatorResult validate(Jwt token) {
            OAuth2TokenValidatorResult porDefecto = JwtValidators.createDefault().validate(token);
            if (porDefecto.hasErrors()) {
                return porDefecto;
            }

            Object emisor = token.getClaim(JwtClaimNames.ISS);
            if (emisor == null || !emisores.contains(emisor.toString())) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_issuer", "El token no lo ha emitido Google: " + emisor, null));
            }

            return OAuth2TokenValidatorResult.success();
        }
    }
}
