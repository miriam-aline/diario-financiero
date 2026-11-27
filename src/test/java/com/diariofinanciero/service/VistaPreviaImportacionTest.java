package com.diariofinanciero.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VistaPreviaImportacionTest {

    @Test
    @DisplayName("Debe exponer contadores, filas y avisos de la vista previa de importación")
    void accessors() {
        Instant ahora = Instant.now();
        VistaPreviaImportacion vista = new VistaPreviaImportacion(
                "tok-1", 1L, "Débito", ahora, 10, 7, 2, 1, 3, List.of(), List.of("Aviso")
        );

        assertEquals("tok-1", vista.tokenConfirmacion());
        assertEquals(1L, vista.cuentaId());
        assertEquals("Débito", vista.nombreCuenta());
        assertEquals(ahora, vista.creadaEn());
        assertEquals(10, vista.totalFilasLeidas());
        assertEquals(7, vista.totalNuevos());
        assertEquals(2, vista.totalDuplicados());
        assertEquals(1, vista.totalOmitidas());
        assertEquals(3, vista.totalSinCategoria());
        assertEquals(List.of("Aviso"), vista.avisosOmitidas());
    }
}
