package com.diariofinanciero.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecurrenteTest {

    private final Categoria categoria = new Categoria("Suscripciones");
    private final Cuenta cuenta = new Cuenta("Débito", TipoCuenta.DEBITO);

    @Nested
    @DisplayName("Construcción y validaciones")
    class ConstruccionTests {

        @Test
        @DisplayName("Debe crear recurrente activo con monto normalizado y diaReferencia tomado de proximoCobro")
        void crearRecurrenteValido() {
            LocalDate proximo = LocalDate.of(2026, 1, 31);
            Recurrente recurrente = new Recurrente(
                    "  Streaming  ",
                    new BigDecimal("199.9"),
                    TipoMovimiento.GASTO,
                    categoria,
                    cuenta,
                    Periodicidad.MENSUAL,
                    proximo
            );

            assertNull(recurrente.getId());
            assertEquals("Streaming", recurrente.getDescripcion());
            assertEquals(new BigDecimal("199.90"), recurrente.getMonto());
            assertEquals(TipoMovimiento.GASTO, recurrente.getTipo());
            assertEquals(categoria, recurrente.getCategoria());
            assertEquals(cuenta, recurrente.getCuenta());
            assertEquals(Periodicidad.MENSUAL, recurrente.getPeriodicidad());
            assertEquals(31, recurrente.getDiaReferencia());
            assertEquals(proximo, recurrente.getProximoCobro());
            assertTrue(recurrente.isActivo());
        }

        @Test
        @DisplayName("Debe validar todos los campos obligatorios y longitud máxima de descripción")
        void validarCamposInvalidos() {
            LocalDate fecha = LocalDate.of(2026, 1, 15);

            assertEquals("La descripción del recurrente es obligatoria",
                    assertThrows(IllegalArgumentException.class, () ->
                            new Recurrente(null, BigDecimal.TEN, TipoMovimiento.GASTO, categoria, cuenta, Periodicidad.MENSUAL, fecha)).getMessage());
            assertEquals("La descripción del recurrente es obligatoria",
                    assertThrows(IllegalArgumentException.class, () ->
                            new Recurrente("  ", BigDecimal.TEN, TipoMovimiento.GASTO, categoria, cuenta, Periodicidad.MENSUAL, fecha)).getMessage());
            assertEquals("La descripción no puede superar 255 caracteres",
                    assertThrows(IllegalArgumentException.class, () ->
                            new Recurrente("x".repeat(256), BigDecimal.TEN, TipoMovimiento.GASTO, categoria, cuenta, Periodicidad.MENSUAL, fecha)).getMessage());

            assertEquals("El tipo de movimiento es obligatorio",
                    assertThrows(IllegalArgumentException.class, () ->
                            new Recurrente("Renta", BigDecimal.TEN, null, categoria, cuenta, Periodicidad.MENSUAL, fecha)).getMessage());
            assertEquals("La categoría es obligatoria",
                    assertThrows(IllegalArgumentException.class, () ->
                            new Recurrente("Renta", BigDecimal.TEN, TipoMovimiento.GASTO, null, cuenta, Periodicidad.MENSUAL, fecha)).getMessage());
            assertEquals("La cuenta es obligatoria",
                    assertThrows(IllegalArgumentException.class, () ->
                            new Recurrente("Renta", BigDecimal.TEN, TipoMovimiento.GASTO, categoria, null, Periodicidad.MENSUAL, fecha)).getMessage());
            assertEquals("La periodicidad es obligatoria",
                    assertThrows(IllegalArgumentException.class, () ->
                            new Recurrente("Renta", BigDecimal.TEN, TipoMovimiento.GASTO, categoria, cuenta, null, fecha)).getMessage());
            assertEquals("La fecha de próximo cobro es obligatoria",
                    assertThrows(IllegalArgumentException.class, () ->
                            new Recurrente("Renta", BigDecimal.TEN, TipoMovimiento.GASTO, categoria, cuenta, Periodicidad.MENSUAL, null)).getMessage());

            assertThrows(IllegalArgumentException.class, () ->
                    new Recurrente("Renta", BigDecimal.ZERO, TipoMovimiento.GASTO, categoria, cuenta, Periodicidad.MENSUAL, fecha));
        }
    }

    @Test
    @DisplayName("Debe avanzar proximoCobro respetando diaReferencia y alternar estado activo")
    void avanzarYAlternarActivo() {
        Recurrente recurrente = new Recurrente(
                "Renta",
                new BigDecimal("5000.00"),
                TipoMovimiento.GASTO,
                categoria,
                cuenta,
                Periodicidad.MENSUAL,
                LocalDate.of(2026, 1, 31)
        );

        recurrente.avanzarProximoCobro();
        assertEquals(LocalDate.of(2026, 2, 28), recurrente.getProximoCobro());

        recurrente.avanzarProximoCobro();
        assertEquals(LocalDate.of(2026, 3, 31), recurrente.getProximoCobro());

        assertTrue(recurrente.isActivo());
        recurrente.alternarActivo();
        assertFalse(recurrente.isActivo());
        recurrente.alternarActivo();
        assertTrue(recurrente.isActivo());
    }
}
