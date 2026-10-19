package dev.pimon.dining.entity;

/** Peticiones especiales habituales de una reserva de restaurante. */
public enum SpecialRequest {
    TRONA,          // bebé / silla alta
    ACCESIBLE,      // acceso para silla de ruedas
    TERRAZA,        // preferencia de terraza
    INTERIOR,       // preferencia de interior
    TRANQUILA,      // zona tranquila
    CELEBRACION,    // cumpleaños/aniversario
    ALERGIAS        // hay alergias/intolerancias (detalle en notas)
}
