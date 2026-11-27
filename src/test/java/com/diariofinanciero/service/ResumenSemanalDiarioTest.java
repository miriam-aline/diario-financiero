package com.diariofinanciero.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResumenSemanalDiarioTest {

    @Test
    @DisplayName("Debe exponer los totales y listas del resumen semanal de diario")
    void accessors() {
        LocalDate inicio = LocalDate.of(2026, 5, 4);
        LocalDate fin = LocalDate.of(2026, 5, 10);
        ResumenSemanalDiario resumen = new ResumenSemanalDiario(
                inicio, fin, new BigDecimal("400.00"), new BigDecimal("1000.00"),
                "Comida", new BigDecimal("300.00"), 4, 7, List.of(), List.of()
        );

        assertEquals(inicio, resumen.inicioSemana());
        assertEquals(fin, resumen.finSemana());
        assertEquals("Comida", resumen.categoriaMasAlta());
        assertEquals(4, resumen.diasSinGasto());
        assertEquals(7, resumen.diasEvaluados());
    }
}
