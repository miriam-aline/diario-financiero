package com.diariofinanciero.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CicloFacturacionTest {

    @Nested
    @DisplayName("fechaEnMes")
    class FechaEnMesTests {

        @Test
        @DisplayName("Debe ajustar el día al último día válido en meses de 30 días, febrero normal y febrero bisiesto")
        void ajustarDiasDeFinDeMes() {
            assertEquals(LocalDate.of(2026, 3, 31), CicloFacturacion.fechaEnMes(YearMonth.of(2026, 3), 31));
            assertEquals(LocalDate.of(2026, 4, 30), CicloFacturacion.fechaEnMes(YearMonth.of(2026, 4), 31));
            assertEquals(LocalDate.of(2026, 2, 28), CicloFacturacion.fechaEnMes(YearMonth.of(2026, 2), 31));
            assertEquals(LocalDate.of(2024, 2, 29), CicloFacturacion.fechaEnMes(YearMonth.of(2024, 2), 30));
            assertEquals(LocalDate.of(2026, 5, 15), CicloFacturacion.fechaEnMes(YearMonth.of(2026, 5), 15));
        }
    }

    @Nested
    @DisplayName("paraMesDeCorte")
    class ParaMesDeCorteTests {

        @Test
        @DisplayName("Debe ubicar fechaPago en el mismo mes cuando diaPago es mayor que diaCorte")
        void pagoEnMismoMes() {
            CicloFacturacion ciclo = CicloFacturacion.paraMesDeCorte(YearMonth.of(2026, 5), 10, 25);

            assertEquals(LocalDate.of(2026, 4, 11), ciclo.inicio());
            assertEquals(LocalDate.of(2026, 5, 10), ciclo.fechaCorte());
            assertEquals(LocalDate.of(2026, 5, 25), ciclo.fechaPago());
        }

        @Test
        @DisplayName("Debe ubicar fechaPago en el mes siguiente cuando diaPago es menor o igual que diaCorte")
        void pagoEnMesSiguiente() {
            CicloFacturacion cicloMenor = CicloFacturacion.paraMesDeCorte(YearMonth.of(2026, 5), 20, 5);
            assertEquals(LocalDate.of(2026, 4, 21), cicloMenor.inicio());
            assertEquals(LocalDate.of(2026, 5, 20), cicloMenor.fechaCorte());
            assertEquals(LocalDate.of(2026, 6, 5), cicloMenor.fechaPago());

            CicloFacturacion cicloIgual = CicloFacturacion.paraMesDeCorte(YearMonth.of(2026, 5), 15, 15);
            assertEquals(LocalDate.of(2026, 4, 16), cicloIgual.inicio());
            assertEquals(LocalDate.of(2026, 5, 15), cicloIgual.fechaCorte());
            assertEquals(LocalDate.of(2026, 6, 15), cicloIgual.fechaPago());
        }

        @Test
        @DisplayName("Debe manejar cortes de día 31 en febrero y cruces de año")
        void ajusteMesCortoYCruceDeAnio() {
            CicloFacturacion marzo = CicloFacturacion.paraMesDeCorte(YearMonth.of(2026, 3), 31, 5);
            assertEquals(LocalDate.of(2026, 3, 1), marzo.inicio());
            assertEquals(LocalDate.of(2026, 3, 31), marzo.fechaCorte());
            assertEquals(LocalDate.of(2026, 4, 5), marzo.fechaPago());

            CicloFacturacion diciembre = CicloFacturacion.paraMesDeCorte(YearMonth.of(2026, 12), 20, 10);
            assertEquals(LocalDate.of(2026, 11, 21), diciembre.inicio());
            assertEquals(LocalDate.of(2026, 12, 20), diciembre.fechaCorte());
            assertEquals(LocalDate.of(2027, 1, 10), diciembre.fechaPago());

            CicloFacturacion febreroBisiesto = CicloFacturacion.paraMesDeCorte(YearMonth.of(2024, 2), 31, 31);
            assertEquals(LocalDate.of(2024, 2, 1), febreroBisiesto.inicio());
            assertEquals(LocalDate.of(2024, 2, 29), febreroBisiesto.fechaCorte());
            assertEquals(LocalDate.of(2024, 3, 31), febreroBisiesto.fechaPago());
        }

        @Test
        @DisplayName("Debe validar que diaCorte y diaPago estén entre 1 y 31")
        void validarDiasFueraDeRango() {
            IllegalArgumentException exCorteBajo = assertThrows(
                    IllegalArgumentException.class,
                    () -> CicloFacturacion.paraMesDeCorte(YearMonth.of(2026, 5), 0, 15)
            );
            assertEquals("El día de corte debe estar entre 1 y 31", exCorteBajo.getMessage());

            IllegalArgumentException exPagoAlto = assertThrows(
                    IllegalArgumentException.class,
                    () -> CicloFacturacion.paraMesDeCorte(YearMonth.of(2026, 5), 15, 32)
            );
            assertEquals("El día de pago debe estar entre 1 y 31", exPagoAlto.getMessage());
        }
    }

    @Nested
    @DisplayName("cicloActual, anterior y días restantes")
    class CicloActualYNavegacionTests {

        @Test
        @DisplayName("Debe permanecer en el mes de referencia antes o en el día de corte y avanzar después del corte")
        void cicloActualSegunFechaReferencia() {
            CicloFacturacion antesCorte = CicloFacturacion.cicloActual(15, 25, LocalDate.of(2026, 5, 10));
            assertEquals(LocalDate.of(2026, 5, 15), antesCorte.fechaCorte());

            CicloFacturacion mismoDiaCorte = CicloFacturacion.cicloActual(15, 25, LocalDate.of(2026, 5, 15));
            assertEquals(LocalDate.of(2026, 5, 15), mismoDiaCorte.fechaCorte());

            CicloFacturacion despuesCorte = CicloFacturacion.cicloActual(15, 25, LocalDate.of(2026, 5, 16));
            assertEquals(LocalDate.of(2026, 5, 16), despuesCorte.inicio());
            assertEquals(LocalDate.of(2026, 6, 15), despuesCorte.fechaCorte());
            assertEquals(LocalDate.of(2026, 6, 25), despuesCorte.fechaPago());
        }

        @Test
        @DisplayName("Debe usar la fecha actual cuando fechaReferencia es null")
        void cicloActualFechaReferenciaNull() {
            LocalDate hoy = LocalDate.now();
            CicloFacturacion ciclo = CicloFacturacion.cicloActual(15, 25, null);

            assertFalse(ciclo.inicio().isAfter(hoy));
            assertFalse(ciclo.fechaCorte().isBefore(hoy));
        }

        @Test
        @DisplayName("Debe calcular el ciclo anterior y los días hasta corte y pago")
        void cicloAnteriorYDiasHasta() {
            CicloFacturacion actual = CicloFacturacion.paraMesDeCorte(YearMonth.of(2026, 3), 10, 25);
            CicloFacturacion anterior = actual.anterior(10, 25);

            assertEquals(LocalDate.of(2026, 1, 11), anterior.inicio());
            assertEquals(LocalDate.of(2026, 2, 10), anterior.fechaCorte());
            assertEquals(LocalDate.of(2026, 2, 25), anterior.fechaPago());

            assertEquals(5L, actual.diasHastaCorte(LocalDate.of(2026, 3, 5)));
            assertEquals(0L, actual.diasHastaCorte(LocalDate.of(2026, 3, 10)));
            assertEquals(-2L, actual.diasHastaCorte(LocalDate.of(2026, 3, 12)));

            assertEquals(15L, actual.diasHastaPago(LocalDate.of(2026, 3, 10)));
            assertEquals(-1L, actual.diasHastaPago(LocalDate.of(2026, 3, 26)));
        }

        @Test
        @DisplayName("Debe validar los días en cicloActual")
        void cicloActualDiasInvalidos() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> CicloFacturacion.cicloActual(32, 10, LocalDate.of(2026, 5, 1))
            );
            assertThrows(
                    IllegalArgumentException.class,
                    () -> CicloFacturacion.cicloActual(10, 0, LocalDate.of(2026, 5, 1))
            );
        }
    }
}
