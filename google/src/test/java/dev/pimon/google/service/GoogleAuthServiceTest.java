package dev.pimon.google.service;

import dev.pimon.common.exception.AppException;
import dev.pimon.google.entity.CuentaGoogle;
import dev.pimon.google.repository.CuentaGoogleRepository;
import dev.pimon.google.service.VerificadorTokenGoogle.IdentidadGoogle;
import dev.pimon.security.config.SecurityProperties;
import dev.pimon.security.dto.LoginResponse;
import dev.pimon.security.jwt.JwtTokenProvider;
import dev.pimon.users.entity.AppUser;
import dev.pimon.users.entity.Role;
import dev.pimon.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Entrar con Google.
 *
 * Lo que se prueba aquí no es que el login funcione —eso es lo fácil— sino a qué cuenta
 * acaba yendo cada token: vincular mal significa meter a alguien en la cuenta de otro.
 */
class GoogleAuthServiceTest {

    private static final String SUB      = "sub-de-google";
    private static final String EMAIL    = "adrian@gmail.com";
    private static final String NOMBRE   = "Adrián";
    private static final String ID_TOKEN = "el-id-token";
    private static final String USER_ID  = "usuario-1";

    private VerificadorTokenGoogle verificador;
    private CuentaGoogleRepository cuentas;
    private UserRepository         usuarios;
    private PasswordEncoder        encoder;
    private JwtTokenProvider       jwt;
    private GoogleAuthService      servicio;

    @BeforeEach
    void preparar() {
        verificador = mock(VerificadorTokenGoogle.class);
        cuentas     = mock(CuentaGoogleRepository.class);
        usuarios    = mock(UserRepository.class);
        encoder     = mock(PasswordEncoder.class);
        jwt         = mock(JwtTokenProvider.class);

        SecurityProperties securityProps = new SecurityProperties();
        securityProps.setAccessTokenExpiration(2592000000L);

        servicio = new GoogleAuthService(verificador, cuentas, usuarios, encoder, jwt,
                securityProps);

        when(verificador.verificar(ID_TOKEN)).thenReturn(new IdentidadGoogle(SUB, EMAIL, NOMBRE));
        when(encoder.encode(anyString())).thenReturn("hash");
        when(jwt.generateToken(anyString(), anyString(), any())).thenReturn("jwt-de-la-app");
    }

    @Test
    @DisplayName("si no hay cuenta con ese correo, se crea una nueva")
    void creaCuentaNueva() {
        when(cuentas.findBySub(SUB)).thenReturn(Optional.empty());
        when(usuarios.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(usuarios.save(any(AppUser.class))).thenAnswer(llamada -> {
            AppUser guardado = llamada.getArgument(0);
            guardado.setId(USER_ID);
            return guardado;
        });

        LoginResponse sesion = servicio.entrar(ID_TOKEN);

        ArgumentCaptor<AppUser> creado = ArgumentCaptor.forClass(AppUser.class);
        verify(usuarios).save(creado.capture());
        assertThat(creado.getValue().getEmail()).isEqualTo(EMAIL);
        assertThat(creado.getValue().getName()).isEqualTo(NOMBRE);
        assertThat(creado.getValue().getRoles()).containsExactly(Role.ROLE_USER);
        assertThat(sesion.token()).isEqualTo("jwt-de-la-app");
        verify(cuentas).save(any(CuentaGoogle.class));
    }

    @Test
    @DisplayName("la contraseña que se le pone es aleatoria y distinta en cada cuenta")
    void contrasenaImposible() {
        when(cuentas.findBySub(SUB)).thenReturn(Optional.empty());
        when(usuarios.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(usuarios.save(any(AppUser.class))).thenAnswer(l -> l.getArgument(0));

        servicio.entrar(ID_TOKEN);
        servicio.entrar(ID_TOKEN);

        // No se guarda ningún original en ninguna parte, así que no hay contraseña que
        // acertar. Y que las dos sean distintas confirma que no hay un valor fijo
        // escondido, que sería lo mismo que dejar la puerta abierta en todas las cuentas.
        ArgumentCaptor<String> passwords = ArgumentCaptor.forClass(String.class);
        verify(encoder, org.mockito.Mockito.times(2)).encode(passwords.capture());
        assertThat(passwords.getAllValues().get(0))
                .isNotEqualTo(passwords.getAllValues().get(1));
    }

    @Test
    @DisplayName("si ya existe una cuenta con ese correo, se vincula en lugar de duplicar")
    void vinculaCuentaExistente() {
        AppUser existente = usuarioActivo();
        when(cuentas.findBySub(SUB)).thenReturn(Optional.empty());
        when(usuarios.findByEmail(EMAIL)).thenReturn(Optional.of(existente));

        LoginResponse sesion = servicio.entrar(ID_TOKEN);

        // Ni se crea otra cuenta ni se toca la contraseña de la que ya había: quien se
        // registró con contraseña sigue pudiendo entrar con ella.
        verify(usuarios, never()).save(any(AppUser.class));
        verify(cuentas).save(any(CuentaGoogle.class));
        assertThat(sesion.id()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("si ya se había entrado antes con Google, se reutiliza el vínculo")
    void vinculoExistente() {
        AppUser existente = usuarioActivo();
        when(cuentas.findBySub(SUB))
                .thenReturn(Optional.of(new CuentaGoogle(USER_ID, SUB, EMAIL, Instant.now())));
        when(usuarios.findById(USER_ID)).thenReturn(Optional.of(existente));

        servicio.entrar(ID_TOKEN);

        verify(usuarios, never()).save(any(AppUser.class));
        // El vínculo ya estaba y el correo no ha cambiado: no hay nada que escribir.
        verify(cuentas, never()).save(any(CuentaGoogle.class));
    }

    @Test
    @DisplayName("si esa persona cambió su correo en Google, se actualiza la referencia")
    void correoCambiadoEnGoogle() {
        AppUser existente = usuarioActivo();
        CuentaGoogle vinculo = new CuentaGoogle(USER_ID, SUB, "antiguo@gmail.com", Instant.now());
        when(cuentas.findBySub(SUB)).thenReturn(Optional.of(vinculo));
        when(usuarios.findById(USER_ID)).thenReturn(Optional.of(existente));

        servicio.entrar(ID_TOKEN);

        // Se busca por `sub` justamente para que esto funcione: sigue siendo la misma
        // persona y entra en su cuenta de siempre, no en una nueva.
        assertThat(vinculo.getEmail()).isEqualTo(EMAIL);
        verify(cuentas).save(vinculo);
    }

    @Test
    @DisplayName("una cuenta dada de baja no puede entrar con Google")
    void cuentaDadaDeBaja() {
        AppUser inactivo = usuarioActivo();
        inactivo.setActive(false);
        when(cuentas.findBySub(SUB)).thenReturn(Optional.empty());
        when(usuarios.findByEmail(EMAIL)).thenReturn(Optional.of(inactivo));

        // Mismo criterio que el login normal. Si se dejara pasar, entrar con Google sería
        // una forma de saltarse la baja.
        assertThatThrownBy(() -> servicio.entrar(ID_TOKEN))
                .isInstanceOf(AppException.class);
    }

    // En gasofa había aquí un test más: que con el login de Google apagado el servicio no
    // hacía nada. Ya no aplica, y es una mejora: la condición vive ahora en la
    // autoconfiguración, así que con `pimon.google.habilitado=false` este bean NO EXISTE —ni
    // el endpoint, ni la entidad—. No se puede llamar a un servicio que no se ha creado.

    private static AppUser usuarioActivo() {
        AppUser usuario = new AppUser(EMAIL, "hash-de-su-contrasena", NOMBRE, Set.of(Role.ROLE_USER));
        usuario.setId(USER_ID);
        return usuario;
    }
}
