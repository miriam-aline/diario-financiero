package com.diariofinanciero.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TipoMovimientoTest {

    @Test
    @DisplayName("Debe definir INGRESO y GASTO con sus etiquetas")
    void valoresYEtiquetas() {
        assertArrayEquals(
                new TipoMovimiento[]{TipoMovimiento.INGRESO, TipoMovimiento.GASTO},
                TipoMovimiento.values()
        );
        assertEquals("Ingreso", TipoMovimiento.INGRESO.getEtiqueta());
        assertEquals("Gasto", TipoMovimiento.GASTO.getEtiqueta());
        assertEquals(TipoMovimiento.INGRESO, TipoMovimiento.valueOf("INGRESO"));
        assertEquals(TipoMovimiento.GASTO, TipoMovimiento.valueOf("GASTO"));
        assertThrows(IllegalArgumentException.class, () -> TipoMovimiento.valueOf("TRASPASO"));
    }
}
