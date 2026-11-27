package com.diariofinanciero.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TipoCuentaTest {

    @Test
    @DisplayName("Debe definir EFECTIVO, DEBITO y CREDITO con sus etiquetas")
    void valoresYEtiquetas() {
        assertArrayEquals(
                new TipoCuenta[]{TipoCuenta.EFECTIVO, TipoCuenta.DEBITO, TipoCuenta.CREDITO},
                TipoCuenta.values()
        );
        assertEquals("Efectivo", TipoCuenta.EFECTIVO.getEtiqueta());
        assertEquals("Débito", TipoCuenta.DEBITO.getEtiqueta());
        assertEquals("Crédito", TipoCuenta.CREDITO.getEtiqueta());
        assertEquals(TipoCuenta.CREDITO, TipoCuenta.valueOf("CREDITO"));
        assertThrows(IllegalArgumentException.class, () -> TipoCuenta.valueOf("AHORRO"));
    }
}
