package com.diariofinanciero.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguracionCsvTest {

    @Test
    @DisplayName("Debe limpiar campos válidos y permitir solo cargo o solo abono")
    void creacionValida() {
        ConfiguracionCsv config = new ConfiguracionCsv(
                1L, " Fecha ", " Desc ", " Cargo ", "   ", " dd/MM/yyyy ", ',', true
        );

        assertEquals(1L, config.cuentaId());
        assertEquals("Fecha", config.columnaFecha());
        assertEquals("Desc", config.columnaDescripcion());
        assertEquals("Cargo", config.columnaCargo());
        assertNull(config.columnaAbono());
        assertEquals("dd/MM/yyyy", config.formatoFecha());
        assertEquals(',', config.separador());
        assertTrue(config.primeraFilaEsEncabezado());
    }

    @Test
    @DisplayName("Debe validar cuenta, columnas obligatorias, al menos cargo o abono y formatoFecha")
    void validaciones() {
        assertThrows(IllegalArgumentException.class, () ->
                new ConfiguracionCsv(null, "F", "D", "C", "A", "dd/MM/yyyy", ',', true));
        assertThrows(IllegalArgumentException.class, () ->
                new ConfiguracionCsv(1L, " ", "D", "C", "A", "dd/MM/yyyy", ',', true));
        assertThrows(IllegalArgumentException.class, () ->
                new ConfiguracionCsv(1L, "F", null, "C", "A", "dd/MM/yyyy", ',', true));
        assertThrows(IllegalArgumentException.class, () ->
                new ConfiguracionCsv(1L, "F", "D", null, "  ", "dd/MM/yyyy", ',', true));
        assertThrows(IllegalArgumentException.class, () ->
                new ConfiguracionCsv(1L, "F", "D", "C", null, " ", ',', true));
    }
}
