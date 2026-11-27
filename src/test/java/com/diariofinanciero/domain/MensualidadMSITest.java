package com.diariofinanciero.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MensualidadMSITest {

    @Test
    @DisplayName("Debe exponer todos sus componentes e implementar igualdad estructural")
    void componentesEIgualdad() {
        LocalDate fecha = LocalDate.of(2026, 6, 15);
        MensualidadMSI m1 = new MensualidadMSI(1, fecha, new BigDecimal("500.00"), true);
        MensualidadMSI m2 = new MensualidadMSI(1, fecha, new BigDecimal("500.00"), true);
        MensualidadMSI m3 = new MensualidadMSI(2, fecha.plusMonths(1), new BigDecimal("500.00"), false);

        assertEquals(1, m1.numero());
        assertEquals(fecha, m1.fechaCargo());
        assertEquals(new BigDecimal("500.00"), m1.monto());
        assertTrue(m1.pagada());

        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
        assertNotEquals(m1, m3);
        assertFalse(m3.pagada());
        assertTrue(m1.toString().contains("MensualidadMSI"));
    }
}
