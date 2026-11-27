package com.diariofinanciero.service;

import com.diariofinanciero.domain.CicloFacturacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResumenCicloTarjetaTest {

    @Test
    @DisplayName("Debe exponer todos los campos del resumen de ciclo de tarjeta")
    void accessors() {
        CicloFacturacion ciclo = CicloFacturacion.paraMesDeCorte(YearMonth.of(2026, 5), 15, 5);
        ResumenCicloTarjeta resumen = new ResumenCicloTarjeta(
                ciclo, new BigDecimal("1500.00"), new BigDecimal("200.00"),
                new BigDecimal("1300.00"), 4, 3L, 24L, false, false, false
        );

        assertEquals(ciclo, resumen.ciclo());
        assertEquals(new BigDecimal("1500.00"), resumen.totalCargos());
        assertEquals(new BigDecimal("200.00"), resumen.totalAbonos());
        assertEquals(new BigDecimal("1300.00"), resumen.estimadoAPagar());
        assertEquals(4, resumen.cantidadMovimientos());
        assertEquals(3L, resumen.diasParaCorte());
        assertEquals(24L, resumen.diasParaPago());
        assertFalse(resumen.pagoProximo());
        assertFalse(resumen.pagoHoy());
        assertFalse(resumen.cicloCerrado());
    }
}
