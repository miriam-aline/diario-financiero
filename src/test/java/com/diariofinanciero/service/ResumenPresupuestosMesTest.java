package com.diariofinanciero.service;

import com.diariofinanciero.domain.EstadoPresupuesto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResumenPresupuestosMesTest {

    @Test
    @DisplayName("Debe exponer los totales y categorías del resumen mensual de presupuestos")
    void accessors() {
        YearMonth mayo = YearMonth.of(2026, 5);
        YearMonth abril = YearMonth.of(2026, 4);
        ResumenPresupuestosMes resumen = new ResumenPresupuestosMes(
                mayo, abril, "Mayo 2026", "Abril 2026",
                new BigDecimal("2000.00"), new BigDecimal("1000.00"), new BigDecimal("800.00"),
                new BigDecimal("200.00"), new BigDecimal("50.0"), 50, EstadoPresupuesto.NORMAL, List.of()
        );

        assertEquals(mayo, resumen.mes());
        assertEquals(abril, resumen.mesAnterior());
        assertEquals("Mayo 2026", resumen.etiquetaMes());
        assertEquals("Abril 2026", resumen.etiquetaMesAnterior());
        assertEquals(EstadoPresupuesto.NORMAL, resumen.estadoGlobal());
        assertEquals(50, resumen.porcentajeBarraGlobal());
    }
}
