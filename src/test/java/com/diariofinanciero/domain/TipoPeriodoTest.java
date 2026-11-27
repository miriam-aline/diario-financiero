package com.diariofinanciero.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TipoPeriodoTest {

    @Test
    @DisplayName("Debe contener los tres tipos de periodo en el orden esperado")
    void valoresEnumEnOrden() {
        assertArrayEquals(
                new TipoPeriodo[]{
                        TipoPeriodo.MES,
                        TipoPeriodo.PRIMERA_QUINCENA,
                        TipoPeriodo.SEGUNDA_QUINCENA
                },
                TipoPeriodo.values()
        );
    }

    @Test
    @DisplayName("Debe retornar la etiqueta legible correspondiente para cada tipo de periodo")
    void etiquetasPorTipo() {
        assertEquals("Mes completo", TipoPeriodo.MES.getEtiqueta());
        assertEquals("1ª quincena (1–15)", TipoPeriodo.PRIMERA_QUINCENA.getEtiqueta());
        assertEquals("2ª quincena (16–fin)", TipoPeriodo.SEGUNDA_QUINCENA.getEtiqueta());
    }

    @Test
    @DisplayName("Debe resolver por nombre con valueOf y lanzar excepción ante valores inválidos o null")
    void valueOfComportamiento() {
        assertEquals(TipoPeriodo.MES, TipoPeriodo.valueOf("MES"));
        assertEquals(TipoPeriodo.PRIMERA_QUINCENA, TipoPeriodo.valueOf("PRIMERA_QUINCENA"));
        assertEquals(TipoPeriodo.SEGUNDA_QUINCENA, TipoPeriodo.valueOf("SEGUNDA_QUINCENA"));

        assertThrows(IllegalArgumentException.class, () -> TipoPeriodo.valueOf("SEMANAL"));
        assertThrows(NullPointerException.class, () -> TipoPeriodo.valueOf(null));
    }
}
