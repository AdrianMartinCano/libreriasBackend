package dev.pimon.google.service;

import dev.pimon.common.exception.AppException;
import dev.pimon.google.config.GoogleProperties;
import dev.pimon.google.service.VerificadorTokenGoogle.IdentidadGoogle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verificación del id_token de Google.
 *
 * Esta clase es la que sostiene el login con Google entero, así que los tests van casi
 * todos sobre lo que NO debe colar. Un fallo aquí no es un error de usuario: es que
 * cualquiera puede entrar como cualquiera.
 */
class VerificadorTokenGoogleTest {

    private static final String NUESTRO_CLIENT_ID = "123-miweb.apps.googleusercontent.com";
    private static final String OTRO_CLIENT_ID    = "999-otraapp.apps.googleusercontent.com";
    private static final String TOKEN             = "el-id-token";

    private JwtDecoder             decodificador;
    private VerificadorTokenGoogle verificador;

    @BeforeEach
    void preparar() {
        decodificador = mock(JwtDecoder.class);
        verificador = new VerificadorTokenGoogle(
                decodificador,
                new GoogleProperties(true, NUESTRO_CLIENT_ID,
                        List.of("https://accounts.google.com", "accounts.google.com")));
    }

    @Test
    @DisplayName("un token bueno devuelve el sub, el correo y el nombre")
    void tokenValido() {
        when(decodificador.decode(TOKEN)).thenReturn(token(NUESTRO_CLIENT_ID, true,
                Map.of("email", "adrian@gmail.com", "name", "Adrián")));

        IdentidadGoogle identidad = verificador.verificar(TOKEN);

        assertThat(identidad.sub()).isEqualTo("sub-de-google");
        assertThat(identidad.email()).isEqualTo("adrian@gmail.com");
        assertThat(identidad.nombre()).isEqualTo("Adrián");
    }

    @Test
    @DisplayName("un token emitido para OTRA aplicación se rechaza")
    void audDeOtraApp() {
        // El caso importante: el token está bien firmado por Google y no ha caducado.
        // Lo único que falla es que no era para nosotros. Sin esta comprobación, quien
        // tenga cualquier app registrada en Google podría coger el token de sus propios
        // usuarios y usarlo aquí para entrar como ellos.
        when(decodificador.decode(TOKEN)).thenReturn(token(OTRO_CLIENT_ID, true,
                Map.of("email", "adrian@gmail.com")));

        assertThatThrownBy(() -> verificador.verificar(TOKEN))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("un correo sin verificar se rechaza, aunque el token sea de Google")
    void correoSinVerificar() {
        // Esta es la que protege a quien ya tenía cuenta con contraseña: como la
        // vinculación se hace por correo, aceptar uno sin verificar permitiría
        // apropiarse de la cuenta de otra persona.
        when(decodificador.decode(TOKEN)).thenReturn(token(NUESTRO_CLIENT_ID, false,
                Map.of("email", "victima@gmail.com")));

        assertThatThrownBy(() -> verificador.verificar(TOKEN))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("si falta el claim email_verified también se rechaza")
    void sinClaimDeVerificacion() {
        Jwt sinClaim = Jwt.withTokenValue(TOKEN)
                .header("alg", "RS256")
                .subject("sub-de-google")
                .audience(List.of(NUESTRO_CLIENT_ID))
                .claim("email", "adrian@gmail.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(decodificador.decode(TOKEN)).thenReturn(sinClaim);

        // Ausente no puede tratarse como "supongo que sí": lo seguro es lo contrario.
        assertThatThrownBy(() -> verificador.verificar(TOKEN))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("una firma inválida o un token caducado se rechazan")
    void firmaMala() {
        when(decodificador.decode(TOKEN)).thenThrow(new BadJwtException("firma inválida"));

        assertThatThrownBy(() -> verificador.verificar(TOKEN))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("sin nombre se usa la parte del correo antes de la arroba")
    void sinNombre() {
        when(decodificador.decode(TOKEN)).thenReturn(token(NUESTRO_CLIENT_ID, true,
                Map.of("email", "adrian@gmail.com")));

        assertThat(verificador.verificar(TOKEN).nombre()).isEqualTo("adrian");
    }

    @Test
    @DisplayName("el correo se normaliza a minúsculas para que case con el del registro")
    void correoEnMayusculas() {
        // Google puede devolverlo como lo escribió la persona. Si no se normaliza, quien
        // se registró como adrian@gmail.com se encontraría una cuenta NUEVA al entrar
        // con Google, en vez de la suya.
        when(decodificador.decode(TOKEN)).thenReturn(token(NUESTRO_CLIENT_ID, true,
                Map.of("email", "Adrian@Gmail.com")));

        assertThat(verificador.verificar(TOKEN).email()).isEqualTo("adrian@gmail.com");
    }

    @Test
    @DisplayName("un token sin correo se rechaza")
    void sinCorreo() {
        when(decodificador.decode(TOKEN)).thenReturn(token(NUESTRO_CLIENT_ID, true, Map.of()));

        assertThatThrownBy(() -> verificador.verificar(TOKEN))
                .isInstanceOf(AppException.class);
    }

    /** Un id_token como los que devuelve Google, con lo que interese en cada test. */
    private static Jwt token(String aud, boolean emailVerificado, Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue(TOKEN)
                .header("alg", "RS256")
                .subject("sub-de-google")
                .audience(List.of(aud))
                .claim("email_verified", emailVerificado)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));

        claims.forEach(builder::claim);

        return builder.build();
    }
}
