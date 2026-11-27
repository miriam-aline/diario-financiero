package com.diariofinanciero.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PeriodicidadTest {

    @Test
    @DisplayName("Debe exponer valores y etiquetas")
    void valoresYEtiquetas() {
        assertArrayEquals(
                new Periodicidad[]{Periodicidad.QUINCENAL, Periodicidad.MENSUAL, Periodicidad.ANUAL},
                Periodicidad.values()
        );
        assertEquals("Quincenal", Periodicidad.QUINCENAL.getEtiqueta());
        assertEquals("Mensual", Periodicidad.MENSUAL.getEtiqueta());
        assertEquals("Anual", Periodicidad.ANUAL.getEtiqueta());
    }

    @Nested
    @DisplayName("calcularSiguienteFecha")
    class CalcularSiguienteFechaTests {

        @Test
        @DisplayName("Debe lanzar IllegalArgumentException cuando fechaActual es null")
        void fechaActualNull() {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> Periodicidad.MENSUAL.calcularSiguienteFecha(null, 15)
            );
            assertEquals("La fecha actual es obligatoria", ex.getMessage());
        }

        @Test
        @DisplayName("Debe calcular periodicidad mensual conservando diaReferencia en meses cortos y bisiestos")
        void periodicidadMensual() {
            LocalDate enero = LocalDate.of(2026, 1, 31);
            LocalDate febrero = Periodicidad.MENSUAL.calcularSiguienteFecha(enero, 31);
            LocalDate marzo = Periodicidad.MENSUAL.calcularSiguienteFecha(febrero, 31);
            LocalDate abril = Periodicidad.MENSUAL.calcularSiguienteFecha(marzo, 31);

            assertEquals(LocalDate.of(2026, 2, 28), febrero);
            assertEquals(LocalDate.of(2026, 3, 31), marzo);
            assertEquals(LocalDate.of(2026, 4, 30), abril);

            assertEquals(LocalDate.of(2024, 2, 29),
                    Periodicidad.MENSUAL.calcularSiguienteFecha(LocalDate.of(2024, 1, 31), 31));
            assertEquals(LocalDate.of(2026, 2, 1),
                    Periodicidad.MENSUAL.calcularSiguienteFecha(LocalDate.of(2026, 1, 1), 0));
        }

        @Test
        @DisplayName("Debe calcular periodicidad anual manejando 29 de febrero")
        void periodicidadAnual() {
            assertEquals(LocalDate.of(2025, 2, 28),
                    Periodicidad.ANUAL.calcularSiguienteFecha(LocalDate.of(2024, 2, 29), 29));
            assertEquals(LocalDate.of(2024, 2, 29),
                    Periodicidad.ANUAL.calcularSiguienteFecha(LocalDate.of(2023, 2, 28), 29));
            assertEquals(LocalDate.of(2027, 6, 15),
                    Periodicidad.ANUAL.calcularSiguienteFecha(LocalDate.of(2026, 6, 15), 15));
        }

        @Test
        @DisplayName("Debe alternar entre día 15 y fin de mes cuando la referencia es 15 o >= 28")
        void periodicidadQuincenalEstandar() {
            assertEquals(LocalDate.of(2026, 2, 28),
                    Periodicidad.QUINCENAL.calcularSiguienteFecha(LocalDate.of(2026, 2, 15), 15));
            assertEquals(LocalDate.of(2026, 3, 15),
                    Periodicidad.QUINCENAL.calcularSiguienteFecha(LocalDate.of(2026, 2, 28), 15));
            assertEquals(LocalDate.of(2024, 2, 29),
                    Periodicidad.QUINCENAL.calcularSiguienteFecha(LocalDate.of(2024, 2, 15), 30));
            assertEquals(LocalDate.of(2024, 3, 15),
                    Periodicidad.QUINCENAL.calcularSiguienteFecha(LocalDate.of(2024, 2, 29), 30));
        }

        @Test
        @DisplayName("Debe sumar 15 días cuando la referencia quincenal no es 15 ni >= 28")
        void periodicidadQuincenalFija15Dias() {
            assertEquals(LocalDate.of(2026, 3, 25),
                    Periodicidad.QUINCENAL.calcularSiguienteFecha(LocalDate.of(2026, 3, 10), 10));
            assertEquals(LocalDate.of(2026, 4, 4),
                    Periodicidad.QUINCENAL.calcularSiguienteFecha(LocalDate.of(2026, 3, 20), 20));
        }
    }
}
