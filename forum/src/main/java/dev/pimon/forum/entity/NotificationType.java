package dev.pimon.forum.entity;

/** Motivo de una notificación del foro. */
public enum NotificationType {
    /** Alguien respondió en tu hilo. */
    REPLY,
    /** Alguien citó tu mensaje. */
    QUOTE,
    /** Alguien reaccionó a tu mensaje. */
    REACTION
}
