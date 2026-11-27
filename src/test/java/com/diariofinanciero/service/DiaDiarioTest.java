package com.diariofinanciero.service;

import com.diariofinanciero.domain.EntradaDiario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiaDiarioTest {

    @Test
    @DisplayName("Debe formatear el día en español con inicial mayúscula y exponer componentes")
    void formatearDiaYAccessors() {
        LocalDate fecha = LocalDate.of(2026, 5, 15);
        String etiqueta = DiaDiario.formatearDia(fecha);

        assertEquals("Viernes 15 de mayo de 2026", etiqueta);

        EntradaDiario entrada = new EntradaDiario(fecha, "Nota", 4, null);
        DiaDiario dia = new DiaDiario(fecha, etiqueta, entrada, List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        assertEquals(fecha, dia.fecha());
        assertEquals(etiqueta, dia.etiquetaDia());
        assertEquals(entrada, dia.entrada());
    }
}
