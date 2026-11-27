package com.diariofinanciero.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PeriodoTest {

    @Nested
    @DisplayName("Construcción")
    class ConstruccionTests {

        @Test
        @DisplayName("Debe resolver fechas para mes completo y quincenas en meses de 28, 29, 30 y 31 días")
        void resolverFechasDePeriodo() {
            Periodo febNormal = Periodo.de(YearMonth.of(2026, 2), TipoPeriodo.MES);
            assertEquals(LocalDate.of(2026, 2, 1), febNormal.desde());
            assertEquals(LocalDate.of(2026, 2, 28), febNormal.hasta());

            Periodo febBisiesto = Periodo.de(YearMonth.of(2024, 2), TipoPeriodo.SEGUNDA_QUINCENA);
            assertEquals(LocalDate.of(2024, 2, 16), febBisiesto.desde());
            assertEquals(LocalDate.of(2024, 2, 29), febBisiesto.hasta());

            Periodo primera = Periodo.de(YearMonth.of(2026, 4), TipoPeriodo.PRIMERA_QUINCENA);
            assertEquals(LocalDate.of(2026, 4, 1), primera.desde());
            assertEquals(LocalDate.of(2026, 4, 15), primera.hasta());

            Periodo treintaYUno = Periodo.de(YearMonth.of(2026, 3), TipoPeriodo.SEGUNDA_QUINCENA);
            assertEquals(LocalDate.of(2026, 3, 16), treintaYUno.desde());
            assertEquals(LocalDate.of(2026, 3, 31), treintaYUno.hasta());
        }

        @Test
        @DisplayName("Debe crear el periodo actual usando el YearMonth actual")
        void periodoActual() {
            YearMonth ahora = YearMonth.now();
            Periodo actual = Periodo.actual(TipoPeriodo.MES);
            assertEquals(Periodo.de(ahora, TipoPeriodo.MES), actual);
        }

        @Test
        @DisplayName("Debe validar mes y tipo obligatorios")
        void validarParametrosNulos() {
            assertEquals("El mes del periodo es obligatorio",
                    assertThrows(IllegalArgumentException.class, () -> Periodo.de(null, TipoPeriodo.MES)).getMessage());
            assertEquals("El tipo de periodo es obligatorio",
                    assertThrows(IllegalArgumentException.class, () -> Periodo.de(YearMonth.of(2026, 1), null)).getMessage());
        }
    }

    @Nested
    @DisplayName("Navegación y etiqueta")
    class NavegacionYEtiquetaTests {

        @Test
        @DisplayName("Debe navegar al periodo anterior y siguiente para cada TipoPeriodo")
        void anteriorYSiguiente() {
            Periodo mesEnero = Periodo.de(YearMonth.of(2026, 1), TipoPeriodo.MES);
            assertEquals(Periodo.de(YearMonth.of(2025, 12), TipoPeriodo.MES), mesEnero.anterior());
            assertEquals(Periodo.de(YearMonth.of(2026, 2), TipoPeriodo.MES), mesEnero.siguiente());

            Periodo q1 = Periodo.de(YearMonth.of(2026, 1), TipoPeriodo.PRIMERA_QUINCENA);
            assertEquals(Periodo.de(YearMonth.of(2025, 12), TipoPeriodo.SEGUNDA_QUINCENA), q1.anterior());
            assertEquals(Periodo.de(YearMonth.of(2026, 1), TipoPeriodo.SEGUNDA_QUINCENA), q1.siguiente());

            Periodo q2 = Periodo.de(YearMonth.of(2026, 12), TipoPeriodo.SEGUNDA_QUINCENA);
            assertEquals(Periodo.de(YearMonth.of(2026, 12), TipoPeriodo.PRIMERA_QUINCENA), q2.anterior());
            assertEquals(Periodo.de(YearMonth.of(2027, 1), TipoPeriodo.PRIMERA_QUINCENA), q2.siguiente());
        }

        @Test
        @DisplayName("Debe formatear etiquetas legibles en español")
        void etiquetasFormateadas() {
            assertEquals("Marzo 2026",
                    Periodo.de(YearMonth.of(2026, 3), TipoPeriodo.MES).etiqueta());
            assertEquals("Marzo 2026 · 1ª quincena (1–15)",
                    Periodo.de(YearMonth.of(2026, 3), TipoPeriodo.PRIMERA_QUINCENA).etiqueta());
            assertEquals("Febrero 2024 · 2ª quincena (16–29)",
                    Periodo.de(YearMonth.of(2024, 2), TipoPeriodo.SEGUNDA_QUINCENA).etiqueta());
        }
    }
}
