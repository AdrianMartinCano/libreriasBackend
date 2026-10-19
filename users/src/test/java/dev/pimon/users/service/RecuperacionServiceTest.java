package dev.pimon.users.service;

import dev.pimon.common.exception.AppException;
import dev.pimon.users.config.UsersProperties;
import dev.pimon.users.entity.AppUser;
import dev.pimon.users.entity.TokenRecuperacion;
import dev.pimon.users.event.RecuperacionSolicitada;
import dev.pimon.users.repository.TokenRecuperacionRepository;
import dev.pimon.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Recuperar la contrasena olvidada.
 *
 * <h2>Por que estos tests, y no otros</h2>
 *
 * Es el unico sitio del sistema donde alguien sin sesion puede cambiar una contrasena. Cada
 * test de aqui protege una propiedad que, si se pierde, deja entrar en la cuenta de otro:
 *
 * <ul>
 *   <li>Que no se pueda averiguar quien tiene cuenta probando correos.</li>
 *   <li>Que en la base no quede nunca el token, solo su hash.</li>
 *   <li>Que un token valga una sola vez y caduque.</li>
 *   <li>Que al usar uno mueran los demas de esa cuenta.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class RecuperacionServiceTest {

    @Mock private UserRepository usuarios;
    @Mock private TokenRecuperacionRepository tokens;
    @Mock private PasswordEncoder encoder;
    @Mock private ApplicationEventPublisher eventos;

    private UsersProperties props;
    private RecuperacionService servicio;

    @BeforeEach
    void preparar() {
        props = new UsersProperties();
        props.getRecuperacion().setValidez(Duration.ofHours(1));
        servicio = new RecuperacionService(usuarios, tokens, encoder, props, eventos);
    }

    // ── Pedir el enlace ───────────────────────────────────────────────────

    @Test
    @DisplayName("un correo sin cuenta NO lanza ni avisa: si no, seria un buscador de clientes")
    void unCorreoDesconocidoNoDelataNada() {
        // Si esto lanzara —o publicara un evento distinto, o tardara distinto—, se podrian
        // probar direcciones y apuntar cuales existen.
        when(usuarios.findByEmail("nadie@ejemplo.com")).thenReturn(Optional.empty());

        servicio.solicitar("nadie@ejemplo.com");

        verifyNoInteractions(tokens);
        verifyNoInteractions(eventos);
    }

    @Test
    @DisplayName("una cuenta dada de baja tampoco recibe enlace")
    void unaCuentaInactivaNoRecibeNada() {
        AppUser baja = usuario();
        baja.setActive(false);
        when(usuarios.findByEmail(anyString())).thenReturn(Optional.of(baja));

        servicio.solicitar("ana@ejemplo.com");

        verifyNoInteractions(tokens);
        verifyNoInteractions(eventos);
    }

    @Test
    @DisplayName("en la base se guarda el HASH, nunca el token que va al correo")
    void seGuardaElHashNoElToken() throws Exception {
        // Esta es la propiedad que hace que un volcado de la base no valga para entrar en
        // ninguna cuenta.
        when(usuarios.findByEmail(anyString())).thenReturn(Optional.of(usuario()));

        servicio.solicitar("ana@ejemplo.com");

        ArgumentCaptor<TokenRecuperacion> guardado =
                ArgumentCaptor.forClass(TokenRecuperacion.class);
        verify(tokens).save(guardado.capture());

        ArgumentCaptor<RecuperacionSolicitada> avisado =
                ArgumentCaptor.forClass(RecuperacionSolicitada.class);
        verify(eventos).publishEvent(avisado.capture());

        String token = avisado.getValue().token();
        assertThat(guardado.getValue().getHash())
                .isNotEqualTo(token)
                .isEqualTo(sha256(token));
    }

    @Test
    @DisplayName("cada peticion emite un token distinto")
    void cadaVezUnoDistinto() {
        when(usuarios.findByEmail(anyString())).thenReturn(Optional.of(usuario()));

        servicio.solicitar("ana@ejemplo.com");
        servicio.solicitar("ana@ejemplo.com");

        ArgumentCaptor<RecuperacionSolicitada> avisos =
                ArgumentCaptor.forClass(RecuperacionSolicitada.class);
        verify(eventos, org.mockito.Mockito.times(2)).publishEvent(avisos.capture());

        assertThat(avisos.getAllValues().get(0).token())
                .isNotEqualTo(avisos.getAllValues().get(1).token());
    }

    @Test
    @DisplayName("el evento no enseña el token al imprimirse: un log no debe guardarlo")
    void elEventoNoSeChiva() {
        // Basta con que alguien escriba log.info("{}", evento) para dejar un permiso de
        // entrada en un fichero que se rota, se copia y se lee sin pensar.
        RecuperacionSolicitada evento =
                new RecuperacionSolicitada("ana@ejemplo.com", "Ana", "SECRETO-123");

        assertThat(evento.toString())
                .doesNotContain("SECRETO-123")
                .contains("ana@ejemplo.com");
    }

    // ── Usar el enlace ────────────────────────────────────────────────────

    @Test
    @DisplayName("un token que no existe se rechaza y dice cómo seguir")
    void tokenInventado() {
        when(tokens.findByHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.restablecer("inventado", "nuevaClave1"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Pide uno nuevo");

        verify(usuarios, never()).save(any());
    }

    @Test
    @DisplayName("un token caducado no vale, aunque no se haya usado")
    void tokenCaducado() {
        TokenRecuperacion viejo = new TokenRecuperacion(
                "u1", sha256("t"), Instant.now().minusSeconds(60));
        when(tokens.findByHash(anyString())).thenReturn(Optional.of(viejo));

        assertThatThrownBy(() -> servicio.restablecer("t", "nuevaClave1"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("caducado");

        verify(usuarios, never()).save(any());
    }

    @Test
    @DisplayName("un token ya usado no vale una segunda vez")
    void tokenGastado() {
        TokenRecuperacion gastado = new TokenRecuperacion(
                "u1", sha256("t"), Instant.now().plusSeconds(600));
        gastado.setUsadoEn(Instant.now().minusSeconds(10));
        when(tokens.findByHash(anyString())).thenReturn(Optional.of(gastado));

        assertThatThrownBy(() -> servicio.restablecer("t", "nuevaClave1"))
                .isInstanceOf(AppException.class);

        verify(usuarios, never()).save(any());
    }

    @Test
    @DisplayName("con un token bueno se cambia la contraseña, cifrada")
    void seCambiaLaContrasena() {
        TokenRecuperacion bueno = new TokenRecuperacion(
                "u1", sha256("t"), Instant.now().plusSeconds(600));
        AppUser ana = usuario();
        when(tokens.findByHash(anyString())).thenReturn(Optional.of(bueno));
        when(usuarios.findById("u1")).thenReturn(Optional.of(ana));
        when(encoder.encode("nuevaClave1")).thenReturn("$2a$cifrada");

        servicio.restablecer("t", "nuevaClave1");

        // Cifrada, no en claro: parece obvio y es exactamente el descuido que dejaria las
        // contraseñas legibles en la base.
        assertThat(ana.getPassword()).isEqualTo("$2a$cifrada");
        verify(usuarios).save(ana);
    }

    @Test
    @DisplayName("al usar uno, mueren los demás enlaces vivos de esa cuenta")
    void seLlevaPorDelanteALosHermanos() {
        // Quien pide el enlace tres veces porque el correo tarda se queda si no con dos
        // permisos abiertos DESPUÉS de haber recuperado la cuenta.
        TokenRecuperacion bueno = new TokenRecuperacion(
                "u1", sha256("t"), Instant.now().plusSeconds(600));
        when(tokens.findByHash(anyString())).thenReturn(Optional.of(bueno));
        when(usuarios.findById("u1")).thenReturn(Optional.of(usuario()));
        when(encoder.encode(anyString())).thenReturn("x");

        servicio.restablecer("t", "nuevaClave1");

        assertThat(bueno.getUsadoEn()).isNotNull();
        verify(tokens).invalidarLosDelUsuario(org.mockito.ArgumentMatchers.eq("u1"), any());
    }

    private AppUser usuario() {
        AppUser u = new AppUser();
        u.setId("u1");
        u.setEmail("ana@ejemplo.com");
        u.setName("Ana");
        u.setActive(true);
        return u;
    }

    /** El mismo hash que calcula el servicio, para poder comprobarlo desde fuera. */
    private static String sha256(String texto) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha.digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
