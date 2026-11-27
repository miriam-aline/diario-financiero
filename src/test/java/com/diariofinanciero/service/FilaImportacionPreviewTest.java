package com.diariofinanciero.service;

import com.diariofinanciero.domain.TipoMovimiento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilaImportacionPreviewTest {

    @Test
    @DisplayName("Debe almacenar y exponer los campos de la fila de vista previa")
    void accessors() {
        LocalDate fecha = LocalDate.of(2026, 5, 10);
        FilaImportacionPreview fila = new FilaImportacionPreview(
                2, fecha, "Café", TipoMovimiento.GASTO, new BigDecimal("65.00"),
                1L, "Comida", "cafe", "hash-1", false, false
        );

        assertEquals(2, fila.numeroLinea());
        assertEquals(fecha, fila.fecha());
        assertEquals("Café", fila.descripcion());
        assertEquals(TipoMovimiento.GASTO, fila.tipo());
        assertEquals(new BigDecimal("65.00"), fila.monto());
        assertEquals(1L, fila.categoriaSugeridaId());
        assertEquals("Comida", fila.categoriaSugeridaNombre());
        assertEquals("cafe", fila.reglaAplicada());
        assertEquals("hash-1", fila.hashImportacion());
        assertFalse(fila.duplicado());
        assertFalse(fila.sinCategoria());

        FilaImportacionPreview dup = new FilaImportacionPreview(
                3, fecha, "Otro", TipoMovimiento.INGRESO, BigDecimal.TEN,
                2L, "Sin categoría", null, "hash-2", true, true
        );
        assertTrue(dup.duplicado());
        assertTrue(dup.sinCategoria());
    }
}
