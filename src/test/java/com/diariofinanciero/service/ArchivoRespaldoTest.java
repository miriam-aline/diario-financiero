package com.diariofinanciero.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArchivoRespaldoTest {

    @Test
    @DisplayName("Debe almacenar y exponer los metadatos del archivo de respaldo")
    void accessors() {
        LocalDateTime fecha = LocalDateTime.of(2026, 5, 1, 10, 30);
        ArchivoRespaldo archivo = new ArchivoRespaldo("respaldo.zip", fecha, 2048L, "2.0 KB");

        assertEquals("respaldo.zip", archivo.nombreArchivo());
        assertEquals(fecha, archivo.fechaCreacion());
        assertEquals(2048L, archivo.tamanoBytes());
        assertEquals("2.0 KB", archivo.tamanoLegible());
    }
}
