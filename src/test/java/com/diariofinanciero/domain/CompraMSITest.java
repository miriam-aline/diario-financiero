package com.diariofinanciero.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompraMSITest {

    private final Cuenta tarjeta = new Cuenta("Tarjeta Oro", TipoCuenta.CREDITO, 15, 5);
    private final Categoria categoria = new Categoria("Tecnología");

    @Nested
    @DisplayName("Construcción y validaciones")
    class ConstruccionTests {

        @Test
        @DisplayName("Debe crear compra MSI válida recortando descripción y normalizando monto")
        void crearCompraValida() {
            LocalDate primerCargo = LocalDate.of(2026, 1, 15);
            CompraMSI compra = new CompraMSI(
                    "  Laptop  ",
                    new BigDecimal("12000"),
                    6,
                    2,
                    tarjeta,
                    categoria,
                    primerCargo
            );

            assertNull(compra.getId());
            assertEquals("Laptop", compra.getDescripcion());
            assertEquals(new BigDecimal("12000.00"), compra.getMontoTotal());
            assertEquals(6, compra.getNumeroMensualidades());
            assertEquals(2, compra.getMensualidadesPagadas());
            assertEquals(tarjeta, compra.getCuenta());
            assertEquals(categoria, compra.getCategoria());
            assertEquals(primerCargo, compra.getFechaPrimerCargo());
        }

        @Test
        @DisplayName("Debe permitir categoría null y descripción de 255 caracteres")
        void crearConCategoriaNullYLongitudMaxima() {
            String desc255 = "x".repeat(255);
            CompraMSI compra = new CompraMSI(
                    desc255,
                    new BigDecimal("600.00"),
                    3,
                    0,
                    tarjeta,
                    null,
                    LocalDate.of(2026, 2, 1)
            );

            assertNull(compra.getCategoria());
            assertEquals(desc255, compra.getDescripcion());
        }

        @Test
        @DisplayName("Debe rechazar parámetros inválidos en el constructor")
        void validarParametrosInvalidos() {
            LocalDate fecha = LocalDate.of(2026, 1, 15);

            assertThrows(IllegalArgumentException.class, () ->
                    new CompraMSI(null, new BigDecimal("100"), 3, 0, tarjeta, categoria, fecha));
            assertThrows(IllegalArgumentException.class, () ->
                    new CompraMSI("   ", new BigDecimal("100"), 3, 0, tarjeta, categoria, fecha));
            assertThrows(IllegalArgumentException.class, () ->
                    new CompraMSI("x".repeat(256), new BigDecimal("100"), 3, 0, tarjeta, categoria, fecha));

            assertThrows(IllegalArgumentException.class, () ->
                    new CompraMSI("Compra", new BigDecimal("100"), 1, 0, tarjeta, categoria, fecha));
            assertThrows(IllegalArgumentException.class, () ->
                    new CompraMSI("Compra", new BigDecimal("100"), 3, -1, tarjeta, categoria, fecha));
            assertThrows(IllegalArgumentException.class, () ->
                    new CompraMSI("Compra", new BigDecimal("100"), 3, 4, tarjeta, categoria, fecha));

            assertThrows(IllegalArgumentException.class, () ->
                    new CompraMSI("Compra", new BigDecimal("100"), 3, 0, null, categoria, fecha));
            assertThrows(IllegalArgumentException.class, () ->
                    new CompraMSI("Compra", new BigDecimal("100"), 3, 0, tarjeta, categoria, null));
            assertThrows(IllegalArgumentException.class, () ->
                    new CompraMSI("Compra", BigDecimal.ZERO, 3, 0, tarjeta, categoria, fecha));
        }
    }

    @Nested
    @DisplayName("calcularPlanMensualidades")
    class PlanMensualidadesTests {

        @Test
        @DisplayName("Debe repartir centavos sobrantes en las primeras cuotas para igualar montoTotal")
        void repartirCentavosExactos() {
            CompraMSI compra = new CompraMSI(
                    "Monitor",
                    new BigDecimal("100.00"),
                    3,
                    1,
                    tarjeta,
                    categoria,
                    LocalDate.of(2026, 3, 10)
            );

            List<MensualidadMSI> plan = compra.calcularPlanMensualidades();

            assertEquals(3, plan.size());
            assertEquals(new MensualidadMSI(1, LocalDate.of(2026, 3, 10), new BigDecimal("33.34"), true), plan.get(0));
            assertEquals(new MensualidadMSI(2, LocalDate.of(2026, 4, 10), new BigDecimal("33.33"), false), plan.get(1));
            assertEquals(new MensualidadMSI(3, LocalDate.of(2026, 5, 10), new BigDecimal("33.33"), false), plan.get(2));

            BigDecimal suma = plan.stream().map(MensualidadMSI::monto).reduce(Dinero.CERO, Dinero::sumar);
            assertEquals(new BigDecimal("100.00"), suma);
        }

        @Test
        @DisplayName("Debe ajustar el día 31 al último día válido en meses cortos y años bisiestos")
        void ajustarFechasFinDeMes() {
            CompraMSI compraBisiesto = new CompraMSI(
                    "Seguro",
                    new BigDecimal("300.00"),
                    3,
                    0,
                    tarjeta,
                    null,
                    LocalDate.of(2024, 1, 31)
            );

            List<MensualidadMSI> planBisiesto = compraBisiesto.calcularPlanMensualidades();
            assertEquals(LocalDate.of(2024, 1, 31), planBisiesto.get(0).fechaCargo());
            assertEquals(LocalDate.of(2024, 2, 29), planBisiesto.get(1).fechaCargo());
            assertEquals(LocalDate.of(2024, 3, 31), planBisiesto.get(2).fechaCargo());

            CompraMSI compraNoBisiesto = new CompraMSI(
                    "Celular",
                    new BigDecimal("400.00"),
                    4,
                    0,
                    tarjeta,
                    null,
                    LocalDate.of(2026, 1, 31)
            );
            List<MensualidadMSI> planNoBisiesto = compraNoBisiesto.calcularPlanMensualidades();
            assertEquals(LocalDate.of(2026, 2, 28), planNoBisiesto.get(1).fechaCargo());
            assertEquals(LocalDate.of(2026, 4, 30), planNoBisiesto.get(3).fechaCargo());
        }

        @Test
        @DisplayName("Debe devolver una lista inmutable")
        void planEsInmutable() {
            CompraMSI compra = new CompraMSI(
                    "Silla",
                    new BigDecimal("200.00"),
                    2,
                    0,
                    tarjeta,
                    null,
                    LocalDate.of(2026, 1, 1)
            );

            List<MensualidadMSI> plan = compra.calcularPlanMensualidades();
            assertThrows(UnsupportedOperationException.class, () -> plan.add(plan.get(0)));
        }
    }

    @Nested
    @DisplayName("Seguimiento y pago de mensualidades")
    class SeguimientoYPagosTests {

        @Test
        @DisplayName("Debe calcular avance, montoPagado, saldoPendiente y proximaMensualidad")
        void avanceYSaldos() {
            CompraMSI compra = new CompraMSI(
                    "Audífonos",
                    new BigDecimal("100.00"),
                    3,
                    1,
                    tarjeta,
                    categoria,
                    LocalDate.of(2026, 5, 1)
            );

            assertEquals(2, compra.mensualidadesRestantes());
            assertFalse(compra.estaLiquidada());
            assertEquals(33, compra.porcentajeAvance());
            assertEquals(new BigDecimal("33.34"), compra.montoPagado());
            assertEquals(new BigDecimal("66.66"), compra.saldoPendiente());
            assertEquals(
                    new MensualidadMSI(2, LocalDate.of(2026, 6, 1), new BigDecimal("33.33"), false),
                    compra.proximaMensualidad()
            );
        }

        @Test
        @DisplayName("Debe registrar pagos sucesivos hasta liquidar y lanzar excepción si ya está liquidada")
        void registrarPagosHastaLiquidar() {
            CompraMSI compra = new CompraMSI(
                    "Tablet",
                    new BigDecimal("100.00"),
                    3,
                    1,
                    tarjeta,
                    categoria,
                    LocalDate.of(2026, 5, 1)
            );

            MensualidadMSI segunda = compra.registrarPagoSiguienteMensualidad();
            assertEquals(2, segunda.numero());
            assertEquals(new BigDecimal("33.33"), segunda.monto());
            assertEquals(2, compra.getMensualidadesPagadas());
            assertEquals(1, compra.mensualidadesRestantes());
            assertEquals(67, compra.porcentajeAvance());
            assertEquals(new BigDecimal("66.67"), compra.montoPagado());
            assertEquals(new BigDecimal("33.33"), compra.saldoPendiente());

            MensualidadMSI tercera = compra.registrarPagoSiguienteMensualidad();
            assertEquals(3, tercera.numero());
            assertEquals(new BigDecimal("33.33"), tercera.monto());
            assertTrue(compra.estaLiquidada());
            assertEquals(0, compra.mensualidadesRestantes());
            assertEquals(100, compra.porcentajeAvance());
            assertEquals(new BigDecimal("100.00"), compra.montoPagado());
            assertEquals(new BigDecimal("0.00"), compra.saldoPendiente());
            assertNull(compra.proximaMensualidad());

            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    compra::registrarPagoSiguienteMensualidad
            );
            assertEquals("Esta compra a MSI ya está completamente pagada", ex.getMessage());
        }
    }
}
