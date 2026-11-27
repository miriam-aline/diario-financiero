package com.diariofinanciero.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FiltroMovimientoTest {

    @Test
    @DisplayName("Debe limpiar texto, construir patrón SQL y validar rango de fechas")
    void filtroYPatron() {
        FiltroMovimiento vacio = FiltroMovimiento.vacio();
        assertNull(vacio.desde());
        assertNull(vacio.hasta());
        assertNull(vacio.categoriaId());
        assertNull(vacio.texto());
        assertNull(vacio.patronBusquedaSql());

        FiltroMovimiento conTexto = new FiltroMovimiento(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                3L,
                "  Café Roma  "
        );
        assertEquals("Café Roma", conTexto.texto());
        assertEquals("%café roma%", conTexto.patronBusquedaSql());

        FiltroMovimiento blanco = new FiltroMovimiento(null, null, null, "   ");
        assertNull(blanco.texto());
        assertNull(blanco.patronBusquedaSql());

        assertThrows(IllegalArgumentException.class, () ->
                new FiltroMovimiento(LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 1), null, null));
    }
}
