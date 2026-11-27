package com.diariofinanciero.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class RecursoNoEncontradoExceptionTest {

    @Test
    @DisplayName("Debe ser RuntimeException y conservar el mensaje")
    void mensajeYJerarquia() {
        RecursoNoEncontradoException ex = new RecursoNoEncontradoException("No encontrado");

        assertInstanceOf(RuntimeException.class, ex);
        assertEquals("No encontrado", ex.getMessage());
    }
}
