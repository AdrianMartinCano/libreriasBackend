package dev.pimon.menu.settings;

/**
 * Fuentes seleccionables para la carta. Lista <b>blanca</b> (no CSS libre) por seguridad y
 * consistencia: el front mapea cada valor a una webfont cargada bajo demanda y la aplica vía
 * variables CSS ({@code --menu-heading-font} / {@code --menu-body-font}).
 */
public enum FontChoice {
    SYSTEM,
    SERIF,
    SANS,
    SLAB,
    DISPLAY,
    HANDWRITTEN,
    MONO
}
