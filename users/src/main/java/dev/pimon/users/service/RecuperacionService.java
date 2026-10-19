package dev.pimon.users.service;

import dev.pimon.common.exception.AppException;
import dev.pimon.users.config.UsersProperties;
import dev.pimon.users.entity.AppUser;
import dev.pimon.users.entity.TokenRecuperacion;
import dev.pimon.users.event.RecuperacionSolicitada;
import dev.pimon.users.repository.TokenRecuperacionRepository;
import dev.pimon.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Recuperar la contrasena cuando no se sabe la anterior.
 *
 * <h2>Es el punto mas delicado de las cuentas</h2>
 *
 * Todo lo demas exige una sesion; esto no puede exigirla, porque quien lo usa es
 * precisamente alguien que no puede entrar. Un fallo aqui no es un error de pantalla: es
 * una forma de entrar en la cuenta de otro. De ahi que cada decision este escrita.
 *
 * <h2>1. Siempre se responde lo mismo</h2>
 *
 * Exista el correo o no. Contestar «ese correo no esta registrado» convertiria esta
 * pantalla en un buscador de clientes: se prueban direcciones y se apunta cuales dan
 * distinto. Por eso {@link #solicitar} <b>no devuelve nada ni lanza</b> cuando no encuentra
 * al usuario, y el controlador dice la misma frase en los dos casos.
 *
 * <h2>2. El token se guarda con hash</h2>
 *
 * El original solo existe en el correo del usuario. Ver
 * {@link dev.pimon.users.entity.TokenRecuperacion}.
 *
 * <h2>3. Un solo uso, y se llevan por delante a los hermanos</h2>
 *
 * Al restablecer se marca el usado y se invalidan los demas tokens vivos de esa cuenta.
 * Quien pidiera el enlace tres veces —porque el correo tardaba— dejaria si no dos permisos
 * abiertos despues de haber recuperado la cuenta.
 *
 * <h2>Lo que NO hace, y conviene saberlo</h2>
 *
 * No limita cuantas veces se puede pedir. Alguien puede pedir mil recuperaciones del correo
 * de otro y llenarle la bandeja. No se resuelve aqui porque hacerlo bien necesita contar
 * peticiones por IP y por cuenta —con almacenamiento y ventana temporal— y a medias es peor
 * que no tenerlo. Si algun dia molesta, se pone delante, no dentro.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecuperacionService {

    /**
     * 32 bytes de aleatoriedad, que en Base64 son 43 caracteres.
     *
     * Es mucho mas de lo que hace falta para que no se pueda adivinar probando, y no cuesta
     * nada: el usuario no lo teclea, viene en un enlace.
     */
    private static final int BYTES_DEL_TOKEN = 32;

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UserRepository usuarios;
    private final TokenRecuperacionRepository tokens;
    private final PasswordEncoder encoder;
    private final UsersProperties props;
    private final ApplicationEventPublisher eventos;

    /**
     * Emite un permiso y avisa para que alguien mande el correo.
     *
     * <b>No lanza nunca por no encontrar la cuenta</b>: ver el punto 1 de arriba.
     */
    @Transactional
    public void solicitar(String email) {
        AppUser usuario = usuarios.findByEmail(email.trim().toLowerCase()).orElse(null);

        if (usuario == null) {
            // Nunca se escribe la direccion: el log sirve para ver que alguien esta
            // probando correos, no para dejar por escrito cuales existen.
            log.info("Recuperacion pedida: ese correo no tiene cuenta. No se envia nada.");
            return;
        }

        if (!usuario.isActive()) {
            // Se distingue del caso anterior a proposito. Para quien mira el servidor son
            // dos problemas distintos —uno es alguien equivocandose de correo, el otro es
            // una cuenta dada de baja pidiendo volver— y meterlos en la misma frase hace
            // perder el tiempo buscando un fallo donde no lo hay.
            //
            // Distinguirlos aqui no cuenta nada nuevo: quien puede leer los logs del
            // servidor puede mirar la base de datos. Lo que protege de verdad es que la
            // RESPUESTA sea identica en los dos casos, y lo sigue siendo.
            log.info("Recuperacion pedida para la cuenta {}, que esta desactivada. "
                     + "No se envia enlace: si no, dar de baja a alguien no serviría de nada.",
                     usuario.getId());
            return;
        }

        String token = nuevoToken();

        tokens.save(new TokenRecuperacion(
                usuario.getId(),
                hashDe(token),
                Instant.now().plus(props.getRecuperacion().getValidez())));

        eventos.publishEvent(
                new RecuperacionSolicitada(usuario.getEmail(), usuario.getName(), token));

        log.info("Emitido un token de recuperacion para el usuario {}", usuario.getId());
    }

    /**
     * Cambia la contrasena si el token vale.
     *
     * <h2>Aqui SI se dice que el enlace no sirve</h2>
     *
     * A diferencia de la solicitud, aqui no se filtra nada: quien tiene el token o lo
     * recibio por correo, o se lo esta inventando. Y quien llega con uno caducado necesita
     * que se le diga, o volvera a pulsar el mismo enlace del correo viejo indefinidamente.
     */
    @Transactional
    public void restablecer(String token, String nuevaPassword) {
        TokenRecuperacion permiso = tokens.findByHash(hashDe(token))
                .orElseThrow(() -> AppException.badRequest(
                        "Este enlace no vale. Pide uno nuevo desde la pantalla de entrar."));

        Instant ahora = Instant.now();
        if (!permiso.esValido(ahora)) {
            throw AppException.badRequest(
                    "Este enlace ya se ha usado o ha caducado. Pide uno nuevo.");
        }

        AppUser usuario = usuarios.findById(permiso.getUserId())
                .orElseThrow(() -> AppException.badRequest("Este enlace ya no vale."));

        usuario.setPassword(encoder.encode(nuevaPassword));
        usuarios.save(usuario);

        // El usado y los hermanos, todos de una vez: ver el punto 3.
        permiso.setUsadoEn(ahora);
        tokens.save(permiso);
        tokens.invalidarLosDelUsuario(usuario.getId(), ahora);

        log.info("Contrasena restablecida para el usuario {}", usuario.getId());
    }

    /** Borra los gastados y los caducados. Lo llama quien quiera, cuando quiera. */
    @Transactional
    public int limpiar() {
        return tokens.limpiarCaducados(Instant.now());
    }

    /** Aleatorio de verdad y en Base64 sin relleno, para que quepa en una URL sin escapar. */
    private String nuevoToken() {
        byte[] bytes = new byte[BYTES_DEL_TOKEN];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 en hexadecimal.
     *
     * Sin sal y sin repeticiones a proposito, al reves que con las contrasenas: aqui el
     * original ya es aleatorio de 32 bytes, asi que no hay diccionario que probar. Y hace
     * falta que el mismo token de siempre el mismo hash para poder buscarlo.
     */
    private String hashDe(String token) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 lo exige la especificacion de Java: si falta, algo va muy mal.
            throw new IllegalStateException("Sin SHA-256 no se puede recuperar contrasenas", e);
        }
    }
}
