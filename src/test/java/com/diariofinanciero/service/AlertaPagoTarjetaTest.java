package com.diariofinanciero.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertaPagoTarjetaTest {

    @Test
    @DisplayName("Debe almacenar y exponer correctamente todos los campos de la alerta")
    void creacionYAccessors() {
        LocalDate fechaPago = LocalDate.of(2026, 12, 5);
        BigDecimal estimado = new BigDecimal("3450.75");

        AlertaPagoTarjeta alerta = new AlertaPagoTarjeta(
                10L,
                "Tarjeta Oro",
                fechaPago,
                9L,
                estimado,
                true
        );

        assertEquals(10L, alerta.cuentaId());
        assertEquals("Tarjeta Oro", alerta.nombreTarjeta());
        assertEquals(fechaPago, alerta.fechaPago());
        assertEquals(9L, alerta.diasRestantes());
        assertEquals(estimado, alerta.estimadoAPagar());
        assertTrue(alerta.esCicloCerrado());
    }

    @Test
    @DisplayName("Debe respetar equals y hashCode por valor de componentes")
    void igualdadYHashCode() {
        LocalDate fechaPago = LocalDate.of(2026, 12, 5);
        BigDecimal estimado = new BigDecimal("1200.00");

        AlertaPagoTarjeta alerta1 = new AlertaPagoTarjeta(1L, "BBVA Azul", fechaPago, 0L, estimado, false);
        AlertaPagoTarjeta alerta2 = new AlertaPagoTarjeta(1L, "BBVA Azul", fechaPago, 0L, new BigDecimal("1200.00"), false);
        AlertaPagoTarjeta distintaTarjeta = new AlertaPagoTarjeta(2L, "BBVA Azul", fechaPago, 0L, estimado, false);
        AlertaPagoTarjeta distintoCiclo = new AlertaPagoTarjeta(1L, "BBVA Azul", fechaPago, 0L, estimado, true);

        assertEquals(alerta1, alerta2);
        assertEquals(alerta1.hashCode(), alerta2.hashCode());
        assertNotEquals(alerta1, distintaTarjeta);
        assertNotEquals(alerta1, distintoCiclo);
        assertFalse(alerta1.esCicloCerrado());
    }

    @Test
    @DisplayName("Debe aceptar valores nulos y días negativos sin restricciones adicionales")
    void valoresNulosYDiasNegativos() {
        AlertaPagoTarjeta alerta = new AlertaPagoTarjeta(null, null, null, -2L, null, false);

        assertNull(alerta.cuentaId());
        assertNull(alerta.nombreTarjeta());
        assertNull(alerta.fechaPago());
        assertEquals(-2L, alerta.diasRestantes());
        assertNull(alerta.estimadoAPagar());
        assertFalse(alerta.esCicloCerrado());
    }

    @Test
    @DisplayName("Debe incluir el nombre del record y sus valores en toString")
    void representacionToString() {
        AlertaPagoTarjeta alerta = new AlertaPagoTarjeta(
                5L,
                "Nu",
                LocalDate.of(2026, 11, 30),
                4L,
                new BigDecimal("850.50"),
                true
        );

        String texto = alerta.toString();
        assertTrue(texto.contains("AlertaPagoTarjeta"));
        assertTrue(texto.contains("cuentaId=5"));
        assertTrue(texto.contains("nombreTarjeta=Nu"));
        assertTrue(texto.contains("fechaPago=2026-11-30"));
        assertTrue(texto.contains("diasRestantes=4"));
        assertTrue(texto.contains("estimadoAPagar=850.50"));
        assertTrue(texto.contains("esCicloCerrado=true"));
    }
}
