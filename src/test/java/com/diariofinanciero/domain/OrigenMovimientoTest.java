package com.diariofinanciero.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrigenMovimientoTest {

    @Test
    @DisplayName("Debe definir MANUAL e IMPORTADO con sus etiquetas")
    void valoresYEtiquetas() {
        assertArrayEquals(
                new OrigenMovimiento[]{OrigenMovimiento.MANUAL, OrigenMovimiento.IMPORTADO},
                OrigenMovimiento.values()
        );
        assertEquals("Manual", OrigenMovimiento.MANUAL.getEtiqueta());
        assertEquals("Importado", OrigenMovimiento.IMPORTADO.getEtiqueta());
        assertEquals(OrigenMovimiento.MANUAL, OrigenMovimiento.valueOf("MANUAL"));
        assertThrows(IllegalArgumentException.class, () -> OrigenMovimiento.valueOf("OTRO"));
    }
}
