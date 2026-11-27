package com.diariofinanciero.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MovimientoTest {

    private final Categoria categoria = new Categoria("Comida");
    private final Cuenta cuenta = new Cuenta("Efectivo", TipoCuenta.EFECTIVO);
    private final LocalDate fecha = LocalDate.of(2026, 5, 12);

    @Nested
    @DisplayName("Construcción y fábrica manual")
    class ConstruccionTests {

        @Test
        @DisplayName("Debe crear movimiento manual con monto normalizado y descripción recortada")
        void crearManual() {
            Movimiento mov = Movimiento.manual(
                    fecha,
                    new BigDecimal("250.4"),
                    TipoMovimiento.GASTO,
                    categoria,
                    cuenta,
                    "  Tacos  "
            );

            assertNull(mov.getId());
            assertEquals(fecha, mov.getFecha());
            assertEquals(new BigDecimal("250.40"), mov.getMonto());
            assertEquals(TipoMovimiento.GASTO, mov.getTipo());
            assertEquals(categoria, mov.getCategoria());
            assertEquals(cuenta, mov.getCuenta());
            assertEquals("Tacos", mov.getDescripcion());
            assertEquals(OrigenMovimiento.MANUAL, mov.getOrigen());
            assertNull(mov.getHashImportacion());
        }

        @Test
        @DisplayName("Debe normalizar hashImportacion en constructor completo")
        void crearImportadoConHash() {
            Movimiento conHash = new Movimiento(
                    fecha, new BigDecimal("100"), TipoMovimiento.INGRESO,
                    categoria, cuenta, "Nómina", OrigenMovimiento.IMPORTADO, "  abc123  "
            );
            assertEquals(OrigenMovimiento.IMPORTADO, conHash.getOrigen());
            assertEquals("abc123", conHash.getHashImportacion());

            Movimiento hashBlanco = new Movimiento(
                    fecha, new BigDecimal("100"), TipoMovimiento.INGRESO,
                    categoria, cuenta, "Nómina", OrigenMovimiento.IMPORTADO, "   "
            );
            assertNull(hashBlanco.getHashImportacion());
        }

        @Test
        @DisplayName("Debe validar todos los campos obligatorios y longitud de descripción")
        void validarCamposObligatorios() {
            assertEquals("El origen del movimiento es obligatorio",
                    assertThrows(IllegalArgumentException.class, () ->
                            new Movimiento(fecha, BigDecimal.TEN, TipoMovimiento.GASTO, categoria, cuenta, "Desc", null, null)).getMessage());

            assertEquals("La fecha es obligatoria",
                    assertThrows(IllegalArgumentException.class, () ->
                            Movimiento.manual(null, BigDecimal.TEN, TipoMovimiento.GASTO, categoria, cuenta, "Desc")).getMessage());

            assertEquals("El tipo de movimiento es obligatorio",
                    assertThrows(IllegalArgumentException.class, () ->
                            Movimiento.manual(fecha, BigDecimal.TEN, null, categoria, cuenta, "Desc")).getMessage());

            assertEquals("La categoría es obligatoria",
                    assertThrows(IllegalArgumentException.class, () ->
                            Movimiento.manual(fecha, BigDecimal.TEN, TipoMovimiento.GASTO, null, cuenta, "Desc")).getMessage());

            assertEquals("La cuenta es obligatoria",
                    assertThrows(IllegalArgumentException.class, () ->
                            Movimiento.manual(fecha, BigDecimal.TEN, TipoMovimiento.GASTO, categoria, null, "Desc")).getMessage());

            assertEquals("La descripción es obligatoria",
                    assertThrows(IllegalArgumentException.class, () ->
                            Movimiento.manual(fecha, BigDecimal.TEN, TipoMovimiento.GASTO, categoria, cuenta, null)).getMessage());
            assertEquals("La descripción es obligatoria",
                    assertThrows(IllegalArgumentException.class, () ->
                            Movimiento.manual(fecha, BigDecimal.TEN, TipoMovimiento.GASTO, categoria, cuenta, "  ")).getMessage());

            assertEquals("La descripción no puede superar 255 caracteres",
                    assertThrows(IllegalArgumentException.class, () ->
                            Movimiento.manual(fecha, BigDecimal.TEN, TipoMovimiento.GASTO, categoria, cuenta, "a".repeat(256))).getMessage());

            assertThrows(IllegalArgumentException.class, () ->
                    Movimiento.manual(fecha, BigDecimal.ZERO, TipoMovimiento.GASTO, categoria, cuenta, "Desc"));
        }
    }

    @Test
    @DisplayName("Debe actualizar campos manteniendo origen y hashImportacion")
    void actualizarMovimiento() {
        Movimiento mov = new Movimiento(
                fecha, new BigDecimal("100.00"), TipoMovimiento.GASTO,
                categoria, cuenta, "Inicial", OrigenMovimiento.IMPORTADO, "hash-1"
        );
        Categoria nuevaCat = new Categoria("Transporte");
        Cuenta nuevaCuenta = new Cuenta("Débito", TipoCuenta.DEBITO);
        LocalDate nuevaFecha = fecha.plusDays(3);

        mov.actualizar(nuevaFecha, new BigDecimal("350.555"), TipoMovimiento.INGRESO, nuevaCat, nuevaCuenta, "  Reembolso  ");

        assertEquals(nuevaFecha, mov.getFecha());
        assertEquals(new BigDecimal("350.56"), mov.getMonto());
        assertEquals(TipoMovimiento.INGRESO, mov.getTipo());
        assertEquals(nuevaCat, mov.getCategoria());
        assertEquals(nuevaCuenta, mov.getCuenta());
        assertEquals("Reembolso", mov.getDescripcion());
        assertEquals(OrigenMovimiento.IMPORTADO, mov.getOrigen());
        assertEquals("hash-1", mov.getHashImportacion());
    }
}
