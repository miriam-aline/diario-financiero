package com.diariofinanciero.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EstadoPresupuestoTest {

    @Test
    @DisplayName("Debe exponer etiquetas y prioridades de orden esperadas")
    void etiquetasYPrioridades() {
        assertEquals("Sin presupuesto", EstadoPresupuesto.SIN_PRESUPUESTO.getEtiqueta());
        assertEquals(3, EstadoPresupuesto.SIN_PRESUPUESTO.getPrioridadOrden());

        assertEquals("Normal", EstadoPresupuesto.NORMAL.getEtiqueta());
        assertEquals(2, EstadoPresupuesto.NORMAL.getPrioridadOrden());

        assertEquals("Aviso", EstadoPresupuesto.AVISO.getEtiqueta());
        assertEquals(1, EstadoPresupuesto.AVISO.getPrioridadOrden());

        assertEquals("Excedido", EstadoPresupuesto.EXCEDIDO.getEtiqueta());
        assertEquals(0, EstadoPresupuesto.EXCEDIDO.getPrioridadOrden());
    }

    @Test
    @DisplayName("Debe retornar SIN_PRESUPUESTO cuando presupuesto es null")
    void sinPresupuesto() {
        assertEquals(EstadoPresupuesto.SIN_PRESUPUESTO, EstadoPresupuesto.evaluar(new BigDecimal("500.00"), null));
        assertEquals(EstadoPresupuesto.SIN_PRESUPUESTO, EstadoPresupuesto.evaluar(null, null));
    }

    @Test
    @DisplayName("Debe clasificar NORMAL, AVISO y EXCEDIDO respetando umbrales exactos de 80% y 100%")
    void evaluarUmbrales() {
        BigDecimal presupuesto = new BigDecimal("100.00");

        assertEquals(EstadoPresupuesto.NORMAL, EstadoPresupuesto.evaluar(null, presupuesto));
        assertEquals(EstadoPresupuesto.NORMAL, EstadoPresupuesto.evaluar(BigDecimal.ZERO, presupuesto));
        assertEquals(EstadoPresupuesto.NORMAL, EstadoPresupuesto.evaluar(new BigDecimal("79.99"), presupuesto));

        assertEquals(EstadoPresupuesto.AVISO, EstadoPresupuesto.evaluar(new BigDecimal("80.00"), presupuesto));
        assertEquals(EstadoPresupuesto.AVISO, EstadoPresupuesto.evaluar(new BigDecimal("99.99"), presupuesto));

        assertEquals(EstadoPresupuesto.EXCEDIDO, EstadoPresupuesto.evaluar(new BigDecimal("100.00"), presupuesto));
        assertEquals(EstadoPresupuesto.EXCEDIDO, EstadoPresupuesto.evaluar(new BigDecimal("150.00"), presupuesto));
    }

    @Test
    @DisplayName("Debe lanzar IllegalArgumentException si el presupuesto es cero o negativo")
    void presupuestoNoPositivo() {
        assertThrows(IllegalArgumentException.class, () -> EstadoPresupuesto.evaluar(BigDecimal.TEN, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> EstadoPresupuesto.evaluar(BigDecimal.TEN, new BigDecimal("-1")));
    }
}
