package com.diariofinanciero.service;

import com.diariofinanciero.domain.EstadoPresupuesto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResumenCategoriaPresupuestoTest {

    @Test
    @DisplayName("Debe exponer todos los campos del resumen de presupuesto por categoría")
    void accessors() {
        ResumenCategoriaPresupuesto resumen = new ResumenCategoriaPresupuesto(
                1L, "Comida", new BigDecimal("1000.00"), new BigDecimal("850.00"),
                new BigDecimal("800.00"), new BigDecimal("50.00"), new BigDecimal("6.3"),
                new BigDecimal("85.0"), 85, new BigDecimal("150.00"), EstadoPresupuesto.AVISO
        );

        assertEquals(1L, resumen.categoriaId());
        assertEquals("Comida", resumen.categoriaNombre());
        assertEquals(new BigDecimal("1000.00"), resumen.presupuestoMensual());
        assertEquals(new BigDecimal("850.00"), resumen.gastadoMes());
        assertEquals(85, resumen.porcentajeBarra());
        assertEquals(EstadoPresupuesto.AVISO, resumen.estado());
    }
}
