package com.diariofinanciero.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReglaCategoriaTest {

    private final Categoria alimentos = new Categoria("Alimentos");
    private final Categoria transporte = new Categoria("Transporte");

    @Test
    @DisplayName("Debe crear regla normalizando textoContiene y permitir actualizar categoría")
    void creacionYActualizacion() {
        ReglaCategoria regla = new ReglaCategoria("  Café   Sí  ", alimentos);

        assertNull(regla.getId());
        assertEquals("cafe si", regla.getTextoContiene());
        assertEquals(alimentos, regla.getCategoria());

        regla.actualizarCategoria(transporte);
        assertEquals(transporte, regla.getCategoria());
    }

    @Test
    @DisplayName("Debe detectar coincidencias ignorando mayúsculas, acentos y espacios múltiples")
    void coincidenciaDeDescripcion() {
        ReglaCategoria regla = new ReglaCategoria("Súper Mercado", alimentos);

        assertTrue(regla.coincide("COMPRA EN SUPER   MERCADO CENTRO"));
        assertTrue(regla.coincide("súper mercado"));
        assertFalse(regla.coincide("Farmacia Guadalajara"));
        assertFalse(regla.coincide(null));
        assertFalse(regla.coincide("   "));
    }

    @Test
    @DisplayName("Debe validar texto obligatorio, longitud máxima de 120 y categoría obligatoria")
    void validarParametrosInvalidos() {
        assertEquals("El texto de la regla es obligatorio",
                assertThrows(IllegalArgumentException.class, () -> new ReglaCategoria(null, alimentos)).getMessage());
        assertEquals("El texto de la regla es obligatorio",
                assertThrows(IllegalArgumentException.class, () -> new ReglaCategoria("   ", alimentos)).getMessage());
        assertEquals("El texto de la regla no puede superar 120 caracteres",
                assertThrows(IllegalArgumentException.class, () -> new ReglaCategoria("a".repeat(121), alimentos)).getMessage());

        assertEquals("La categoría de la regla es obligatoria",
                assertThrows(IllegalArgumentException.class, () -> new ReglaCategoria("uber", null)).getMessage());

        ReglaCategoria regla = new ReglaCategoria("uber", transporte);
        assertEquals("La categoría de la regla es obligatoria",
                assertThrows(IllegalArgumentException.class, () -> regla.actualizarCategoria(null)).getMessage());
    }
}
