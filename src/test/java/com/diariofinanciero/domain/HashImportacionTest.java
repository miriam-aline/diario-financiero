package com.diariofinanciero.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HashImportacionTest {

    @Test
    @DisplayName("Debe normalizar texto eliminando acentos, mayúsculas y espacios redundantes")
    void normalizarTexto() {
        assertEquals("", HashImportacion.normalizarTexto(null));
        assertEquals("", HashImportacion.normalizarTexto("   "));
        assertEquals("cafe nandu supermercado", HashImportacion.normalizarTexto("  Café   Ñandú\tSupermercado  "));
    }

    @Test
    @DisplayName("Debe construir claveBase con monto normalizado y texto normalizado")
    void claveBaseFormato() {
        String clave = HashImportacion.claveBase(
                7L,
                LocalDate.of(2026, 4, 15),
                TipoMovimiento.GASTO,
                new BigDecimal("150.5"),
                "  Súper   Mercado "
        );

        assertEquals("7|2026-04-15|GASTO|150.50|super mercado", clave);
    }

    @Test
    @DisplayName("Debe generar SHA-256 determinístico de 64 caracteres diferenciando por ocurrencia y campos")
    void calcularHashDeterministico() {
        LocalDate fecha = LocalDate.of(2026, 4, 15);

        String hash1 = HashImportacion.calcular(1L, fecha, TipoMovimiento.GASTO, new BigDecimal("100"), "Café Roma", 1);
        String hashEquivalente = HashImportacion.calcular(1L, fecha, TipoMovimiento.GASTO, new BigDecimal("100.00"), "  cafe   ROMA ", 1);
        String hashOcurrencia2 = HashImportacion.calcular(1L, fecha, TipoMovimiento.GASTO, new BigDecimal("100.00"), "Café Roma", 2);
        String hashOtroTipo = HashImportacion.calcular(1L, fecha, TipoMovimiento.INGRESO, new BigDecimal("100.00"), "Café Roma", 1);

        assertEquals(64, hash1.length());
        assertEquals(hash1, hashEquivalente);
        assertNotEquals(hash1, hashOcurrencia2);
        assertNotEquals(hash1, hashOtroTipo);
    }

    @Test
    @DisplayName("Debe validar ocurrencia mayor o igual a 1 y monto positivo")
    void validarParametros() {
        LocalDate fecha = LocalDate.of(2026, 4, 15);

        IllegalArgumentException exOcurrencia = assertThrows(
                IllegalArgumentException.class,
                () -> HashImportacion.calcular(1L, fecha, TipoMovimiento.GASTO, BigDecimal.TEN, "Desc", 0)
        );
        assertEquals("La ocurrencia debe ser mayor o igual a 1", exOcurrencia.getMessage());

        assertThrows(
                IllegalArgumentException.class,
                () -> HashImportacion.calcular(1L, fecha, TipoMovimiento.GASTO, BigDecimal.ZERO, "Desc", 1)
        );
    }
}
