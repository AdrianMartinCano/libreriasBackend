package dev.pimon.menu.entity;

/**
 * Los 14 alérgenos de declaración obligatoria en la UE (Reglamento 1169/2011). Se persisten por
 * nombre ({@code EnumType.STRING}) para que añadir valores no rompa los datos existentes.
 */
public enum Allergen {
    GLUTEN,
    CRUSTACEOS,
    HUEVOS,
    PESCADO,
    CACAHUETES,
    SOJA,
    LACTEOS,
    FRUTOS_CASCARA,
    APIO,
    MOSTAZA,
    SESAMO,
    SULFITOS,
    ALTRAMUCES,
    MOLUSCOS
}
