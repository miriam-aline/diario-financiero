package com.diariofinanciero.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DineroTest {

    @Test
    @DisplayName("Debe definir constantes monetarias con escala 2, redondeo HALF_UP y CERO normalizado")
    void constantesMonetarias() {
        assertEquals(2, Dinero.ESCALA);
        assertEquals(RoundingMode.HALF_UP, Dinero.REDONDEO);
        assertEquals(new BigDecimal("0.00"), Dinero.CERO);
        assertEquals(2, Dinero.CERO.scale());
    }

    @Nested
    @DisplayName("normalizar")
    class NormalizarTests {

        @Test
        @DisplayName("Debe ajustar a escala 2 cuando el monto tiene menos decimales")
        void normalizarMenosDecimales() {
            assertEquals(new BigDecimal("10.00"), Dinero.normalizar(new BigDecimal("10")));
            assertEquals(new BigDecimal("10.50"), Dinero.normalizar(new BigDecimal("10.5")));
            assertEquals(new BigDecimal("0.00"), Dinero.normalizar(BigDecimal.ZERO));
            assertEquals(new BigDecimal("-25.10"), Dinero.normalizar(new BigDecimal("-25.1")));
        }

        @Test
        @DisplayName("Debe mantener el valor cuando ya tiene escala 2")
        void normalizarEscalaDos() {
            assertEquals(new BigDecimal("123.45"), Dinero.normalizar(new BigDecimal("123.45")));
        }

        @Test
        @DisplayName("Debe redondear usando HALF_UP cuando tiene más de 2 decimales")
        void normalizarConRedondeoHalfUp() {
            assertEquals(new BigDecimal("10.01"), Dinero.normalizar(new BigDecimal("10.005")));
            assertEquals(new BigDecimal("10.00"), Dinero.normalizar(new BigDecimal("10.004")));
            assertEquals(new BigDecimal("-10.01"), Dinero.normalizar(new BigDecimal("-10.005")));
        }

        @Test
        @DisplayName("Debe lanzar IllegalArgumentException cuando el monto es null")
        void normalizarMontoNull() {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> Dinero.normalizar(null)
            );
            assertEquals("El monto es obligatorio", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("validarPositivo")
    class ValidarPositivoTests {

        @Test
        @DisplayName("Debe retornar el monto normalizado cuando es positivo")
        void validarPositivoValido() {
            assertEquals(new BigDecimal("10.50"), Dinero.validarPositivo(new BigDecimal("10.5")));
            assertEquals(new BigDecimal("0.01"), Dinero.validarPositivo(new BigDecimal("0.01")));
            assertEquals(new BigDecimal("0.01"), Dinero.validarPositivo(new BigDecimal("0.005")));
        }

        @Test
        @DisplayName("Debe lanzar IllegalArgumentException cuando el monto es cero, negativo o redondea a cero")
        void validarPositivoCeroONegativo() {
            IllegalArgumentException exCero = assertThrows(
                    IllegalArgumentException.class,
                    () -> Dinero.validarPositivo(BigDecimal.ZERO)
            );
            assertEquals("El monto debe ser mayor que cero", exCero.getMessage());

            IllegalArgumentException exRedondeaACero = assertThrows(
                    IllegalArgumentException.class,
                    () -> Dinero.validarPositivo(new BigDecimal("0.004"))
            );
            assertEquals("El monto debe ser mayor que cero", exRedondeaACero.getMessage());

            IllegalArgumentException exNegativo = assertThrows(
                    IllegalArgumentException.class,
                    () -> Dinero.validarPositivo(new BigDecimal("-1.00"))
            );
            assertEquals("El monto debe ser mayor que cero", exNegativo.getMessage());
        }

        @Test
        @DisplayName("Debe lanzar IllegalArgumentException cuando el monto es null")
        void validarPositivoNull() {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> Dinero.validarPositivo(null)
            );
            assertEquals("El monto es obligatorio", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("sumar")
    class SumarTests {

        @Test
        @DisplayName("Debe sumar dos montos y retornar escala 2")
        void sumarMontosValidos() {
            assertEquals(new BigDecimal("16.00"), Dinero.sumar(new BigDecimal("10.25"), new BigDecimal("5.75")));
            assertEquals(new BigDecimal("6.50"), Dinero.sumar(new BigDecimal("10"), new BigDecimal("-3.5")));
        }

        @Test
        @DisplayName("Debe normalizar cada operando antes de sumar")
        void sumarNormalizandoOperandos() {
            assertEquals(new BigDecimal("11.02"), Dinero.sumar(new BigDecimal("10.005"), new BigDecimal("1.005")));
        }

        @Test
        @DisplayName("Debe lanzar IllegalArgumentException si algún operando es null")
        void sumarConNull() {
            IllegalArgumentException exPrimero = assertThrows(
                    IllegalArgumentException.class,
                    () -> Dinero.sumar(null, BigDecimal.ONE)
            );
            assertEquals("El monto es obligatorio", exPrimero.getMessage());

            IllegalArgumentException exSegundo = assertThrows(
                    IllegalArgumentException.class,
                    () -> Dinero.sumar(BigDecimal.ONE, null)
            );
            assertEquals("El monto es obligatorio", exSegundo.getMessage());
        }
    }

    @Nested
    @DisplayName("restar")
    class RestarTests {

        @Test
        @DisplayName("Debe restar dos montos y retornar escala 2")
        void restarMontosValidos() {
            assertEquals(new BigDecimal("12.75"), Dinero.restar(new BigDecimal("20.00"), new BigDecimal("7.25")));
            assertEquals(new BigDecimal("0.00"), Dinero.restar(new BigDecimal("10.5"), new BigDecimal("10.50")));
            assertEquals(new BigDecimal("-7.30"), Dinero.restar(new BigDecimal("5"), new BigDecimal("12.3")));
        }

        @Test
        @DisplayName("Debe normalizar cada operando antes de restar")
        void restarNormalizandoOperandos() {
            assertEquals(new BigDecimal("10.01"), Dinero.restar(new BigDecimal("10.005"), new BigDecimal("0.004")));
        }

        @Test
        @DisplayName("Debe lanzar IllegalArgumentException si algún operando es null")
        void restarConNull() {
            IllegalArgumentException exPrimero = assertThrows(
                    IllegalArgumentException.class,
                    () -> Dinero.restar(null, BigDecimal.ONE)
            );
            assertEquals("El monto es obligatorio", exPrimero.getMessage());

            IllegalArgumentException exSegundo = assertThrows(
                    IllegalArgumentException.class,
                    () -> Dinero.restar(BigDecimal.ONE, null)
            );
            assertEquals("El monto es obligatorio", exSegundo.getMessage());
        }
    }
}
