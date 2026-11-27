package com.diariofinanciero.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultadoConfirmacionImportacionTest {

    @Test
    @DisplayName("Debe exponer contadores del resultado de confirmación")
    void accessors() {
        ResultadoConfirmacionImportacion resultado = new ResultadoConfirmacionImportacion(5, 2, 1);

        assertEquals(5, resultado.importados());
        assertEquals(2, resultado.omitidosDuplicados());
        assertEquals(1, resultado.pendientesSinCategoria());
    }
}
