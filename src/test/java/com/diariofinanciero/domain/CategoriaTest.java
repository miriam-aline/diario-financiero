package com.diariofinanciero.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CategoriaTest {

    @Nested
    @DisplayName("Construcción y validación")
    class ConstruccionTests {

        @Test
        @DisplayName("Debe crear categoría sin presupuesto haciendo trim al nombre")
        void crearSinPresupuesto() {
            Categoria categoria = new Categoria("  Alimentación  ");

            assertNull(categoria.getId());
            assertEquals("Alimentación", categoria.getNombre());
            assertNull(categoria.getPresupuestoMensual());
        }

        @Test
        @DisplayName("Debe crear categoría con presupuesto normalizado a escala 2")
        void crearConPresupuesto() {
            Categoria categoria = new Categoria("Transporte", new BigDecimal("1500.5"));

            assertEquals("Transporte", categoria.getNombre());
            assertEquals(new BigDecimal("1500.50"), categoria.getPresupuestoMensual());
        }

        @Test
        @DisplayName("Debe aceptar nombre de exactamente 100 caracteres tras hacer trim")
        void aceptarLongitudMaximaTrasTrim() {
            String nombre100 = "a".repeat(100);
            Categoria categoria = new Categoria("  " + nombre100 + "  ");

            assertEquals(nombre100, categoria.getNombre());
        }

        @Test
        @DisplayName("Debe lanzar IllegalArgumentException cuando el nombre es null, vacío o supera 100 caracteres")
        void validarNombreInvalido() {
            IllegalArgumentException exNull = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Categoria(null)
            );
            assertEquals("El nombre de la categoría es obligatorio", exNull.getMessage());

            IllegalArgumentException exBlank = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Categoria("   ")
            );
            assertEquals("El nombre de la categoría es obligatorio", exBlank.getMessage());

            String nombre101 = "a".repeat(101);
            IllegalArgumentException exLargo = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Categoria(nombre101)
            );
            assertEquals("El nombre de la categoría no puede superar 100 caracteres", exLargo.getMessage());
        }

        @Test
        @DisplayName("Debe lanzar IllegalArgumentException cuando el presupuesto inicial es cero o negativo")
        void validarPresupuestoInicialInvalido() {
            IllegalArgumentException exCero = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Categoria("Ocio", BigDecimal.ZERO)
            );
            assertEquals("El monto debe ser mayor que cero", exCero.getMessage());

            IllegalArgumentException exNegativo = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Categoria("Ocio", new BigDecimal("-10.00"))
            );
            assertEquals("El monto debe ser mayor que cero", exNegativo.getMessage());
        }
    }

    @Nested
    @DisplayName("actualizarPresupuesto")
    class ActualizarPresupuestoTests {

        @Test
        @DisplayName("Debe actualizar el presupuesto normalizado o limpiarlo con null")
        void actualizarPresupuestoYLimpiar() {
            Categoria categoria = new Categoria("Servicios", new BigDecimal("800"));

            categoria.actualizarPresupuesto(new BigDecimal("1250.345"));
            assertEquals(new BigDecimal("1250.35"), categoria.getPresupuestoMensual());

            categoria.actualizarPresupuesto(null);
            assertNull(categoria.getPresupuestoMensual());
        }

        @Test
        @DisplayName("Debe lanzar IllegalArgumentException si el nuevo presupuesto es cero o negativo")
        void actualizarPresupuestoInvalido() {
            Categoria categoria = new Categoria("Servicios", new BigDecimal("800.00"));

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> categoria.actualizarPresupuesto(BigDecimal.ZERO)
            );
            assertEquals("El monto debe ser mayor que cero", ex.getMessage());
            assertEquals(new BigDecimal("800.00"), categoria.getPresupuestoMensual());
        }
    }
}
