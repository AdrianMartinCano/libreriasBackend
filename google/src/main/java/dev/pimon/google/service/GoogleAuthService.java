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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Entrar con Google.
 *
 * <p>El token de Google se usa <b>una sola vez</b>, aquí, y se tira. A partir de ese momento
 * la sesión es el JWT de la aplicación, igual que en el login normal. Eso tiene una
 * consecuencia práctica útil: no hace falta refrescar nada contra Google, y la caducidad de
 * sesión es la nuestra.
 *
 * <p>Los tres casos que puede haber, y qué se hace en cada uno:
 *
 * <ol>
 *   <li><b>Ya se había entrado con Google antes</b> (hay vínculo por {@code sub}) → se entra.
 *       Si esa persona cambió su correo en Google, se actualiza el nuestro.</li>
 *   <li><b>Existe una cuenta con ese correo pero sin vínculo</b> → se vincula. Es el paso
 *       delicado, y solo es seguro porque {@link VerificadorTokenGoogle} ya ha exigido
 *       {@code email_verified}: sin eso, esto permitiría apropiarse de la cuenta de cualquiera
 *       que se hubiera registrado con contraseña.</li>
 *   <li><b>No hay nada</b> → se crea la cuenta. Sin pedir nada más: obligar a rellenar un
 *       formulario después de entrar con Google es justo lo que la gente quería evitar.</li>
 * </ol>
 */
@Slf4j
@Service
public class GoogleAuthService {

    /**
     * Bytes de la contraseña imposible. 32 bytes son 256 bits de aleatoriedad, generados con
     * {@link SecureRandom} y tirados sin guardarlos en ningún sitio: lo que queda en la base es
     * un hash del que no existe original conocido.
     */
    private static final int BYTES_PASSWORD = 32;

    /** Hasta dónde se prueba «Nombre2», «Nombre3»… antes de rendirse y usar algo único. */
    private static final int INTENTOS_NICK = 100;

    private final SecureRandom aleatorio = new SecureRandom();

    private final VerificadorTokenGoogle verificador;
    private final CuentaGoogleRepository cuentas;
    private final UserRepository         usuarios;
    private final PasswordEncoder        encoder;
    private final JwtTokenProvider       jwt;
    private final SecurityProperties     securityProps;

    public GoogleAuthService(VerificadorTokenGoogle verificador, CuentaGoogleRepository cuentas,
                             UserRepository usuarios, PasswordEncoder encoder,
                             JwtTokenProvider jwt, SecurityProperties securityProps) {
        this.verificador   = verificador;
        this.cuentas       = cuentas;
        this.usuarios      = usuarios;
        this.encoder       = encoder;
        this.jwt           = jwt;
        this.securityProps = securityProps;
    }

    /**
     * Verifica el token de Google y devuelve una sesión de la aplicación.
     *
     * <p>No comprueba si el login con Google está encendido, y no le hace falta: con
     * {@code pimon.google.habilitado=false} este bean no llega a existir.
     *
     * @throws AppException si el token no vale o la cuenta está dada de baja
     */
    @Transactional
    public LoginResponse entrar(String idToken) {
        IdentidadGoogle identidad = verificador.verificar(idToken);
        Instant ahora = Instant.now();

        AppUser usuario = cuentas.findBySub(identidad.sub())
                .map(vinculo -> porVinculoExistente(vinculo, identidad))
                .orElseGet(() -> vincularOCrear(identidad, ahora));

        // Se comprueba DESPUES de resolver el usuario y no antes, porque una cuenta dada de
        // baja si existe: hay que reconocerla para poder rechazarla con el mismo criterio que
        // el login normal, en vez de crear otra cuenta con su mismo correo.
        if (!usuario.isActive()) {
            log.info("Intento de entrar con Google en una cuenta dada de baja");
            throw AppException.unauthorized("Esta cuenta está dada de baja.");
        }

        return sesionDe(usuario);
    }

    /** Ya había vínculo: solo hay que refrescar el correo si cambió en Google. */
    private AppUser porVinculoExistente(CuentaGoogle vinculo, IdentidadGoogle identidad) {
        AppUser usuario = usuarios.findById(vinculo.getUserId())
                // El vinculo lleva ON DELETE CASCADE en la migracion, asi que esto no deberia
                // pasar nunca. Si pasa, es un fallo nuestro y no algo que tragarse en silencio.
                .orElseThrow(() -> new IllegalStateException(
                        "Vínculo de Google apuntando a un usuario que no existe: "
                        + vinculo.getUserId()));

        if (!identidad.email().equals(vinculo.getEmail())) {
            log.info("La cuenta de Google vinculada cambió de dirección; se actualiza la referencia");
            vinculo.setEmail(identidad.email());
            cuentas.save(vinculo);
        }

        return usuario;
    }

    /** No había vínculo: o se ata a una cuenta que ya existe, o se crea una nueva. */
    private AppUser vincularOCrear(IdentidadGoogle identidad, Instant ahora) {
        Optional<AppUser> existente = usuarios.findByEmail(identidad.email());

        AppUser usuario = existente.orElseGet(() -> crearCuenta(identidad));

        if (existente.isPresent()) {
            log.info("Se vincula una cuenta ya registrada con su Google (correo verificado)");
        }

        cuentas.save(new CuentaGoogle(usuario.getId(), identidad.sub(), identidad.email(), ahora));

        return usuario;
    }

    /**
     * Crea la cuenta con una contraseña que no conoce nadie.
     *
     * No es un hueco: es que no existe ninguna cadena que produzca ese hash y que alguien pueda
     * escribir. Quien quiera además una contraseña normal puede pedirla por «he olvidado mi
     * contraseña», que llega a su correo — el mismo que Google acaba de verificar que es suyo.
     */
    private AppUser crearCuenta(IdentidadGoogle identidad) {
        byte[] bytes = new byte[BYTES_PASSWORD];
        aleatorio.nextBytes(bytes);
        String passwordImposible = Base64.getEncoder().encodeToString(bytes);

        AppUser nuevo = new AppUser(
                identidad.email(),
                encoder.encode(passwordImposible),
                nickLibre(identidad),
                Set.of(Role.ROLE_USER));

        log.info("Cuenta nueva creada desde Google");

        return usuarios.save(nuevo);
    }

    /**
     * Un nick con el que firmar, a partir de lo que manda Google.
     *
     * <h2>Solo el nombre de pila</h2>
     *
     * Google manda el nombre completo de la cuenta: «Adrián Martín Cano». Ese nombre pasa a
     * ser el nick, y <b>el nick es con lo que se firma en el foro</b>. Nadie entra con Google
     * decidiendo publicar su nombre y sus dos apellidos: entra porque es un clic. Guardar el
     * nombre completo es publicar por esa persona algo que no ha elegido.
     *
     * <h2>Y que no choque con otro</h2>
     *
     * Esta cuenta se crea <b>sin pasar por {@code UserService.register()}</b>, que es donde
     * vive la comprobación de nick único. Si no se comprueba aquí, entrar con Google es la
     * puerta de atrás para acabar con dos «Marta» — y dos Martas en un foro es que ya no se
     * sabe quién dijo qué.
     *
     * <h2>Esto NUNCA puede impedir entrar</h2>
     *
     * Rechazar el login porque el nick está cogido sería echar a alguien de la puerta por un
     * nombre que ni eligió ni sabe que existe. Se prueba «Adrián», «Adrián2», «Adrián3»…
     * y si aun así no hubiera hueco, se cae a algo único de verdad. Después puede cambiarlo
     * desde «Mi cuenta», que ya valida que esté libre.
     */
    private String nickLibre(IdentidadGoogle identidad) {
        String base = primeraPalabra(identidad.nombre());
        if (base.isBlank()) {
            base = primeraPalabra(identidad.email().split("@")[0]);
        }
        if (base.isBlank()) {
            base = "Lector";
        }

        if (!usuarios.existsByNameIgnoreCase(base)) {
            return base;
        }
        for (int n = 2; n <= INTENTOS_NICK; n++) {
            String intento = base + n;
            if (!usuarios.existsByNameIgnoreCase(intento)) {
                return intento;
            }
        }

        // Cien «Adrián» es inverosímil, pero si pasara, entrar sigue siendo más importante
        // que tener un nick bonito: se coge algo que no puede chocar y ya lo cambiará.
        return base + "-" + UUID.randomUUID().toString().substring(0, 6);
    }

    /** «Adrián Martín Cano» → «Adrián». Sin nombre, cadena vacía. */
    private static String primeraPalabra(String nombre) {
        if (nombre == null) {
            return "";
        }
        String limpio = nombre.trim();
        int espacio = limpio.indexOf(' ');
        return espacio > 0 ? limpio.substring(0, espacio) : limpio;
    }

    /** Si esa cuenta entró por Google, y por tanto no tiene contraseña que conozca. */
    @Transactional(readOnly = true)
    public boolean estaVinculada(String userId) {
        return cuentas.existsByUserId(userId);
    }

    /** El mismo JWT que emite el login normal: a partir de aquí, todo funciona igual. */
    private LoginResponse sesionDe(AppUser usuario) {
        List<String> roles = usuario.getRoles().stream().map(Enum::name).toList();

        String token = jwt.generateToken(usuario.getId(), usuario.getEmail(), roles);

        return new LoginResponse(
                token,
                securityProps.getAccessTokenExpiration(),
                usuario.getId(),
                usuario.getEmail(),
                roles);
    }
}
