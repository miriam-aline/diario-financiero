package com.diariofinanciero.service;

import com.diariofinanciero.domain.CicloFacturacion;
import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.TipoCuenta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResumenTarjetaTest {

    @Test
    @DisplayName("Debe exponer los ciclos y movimientos asociados al resumen de tarjeta")
    void accessors() {
        Cuenta cuenta = new Cuenta("Nu", TipoCuenta.CREDITO, 15, 5);
        CicloFacturacion ciclo = CicloFacturacion.paraMesDeCorte(YearMonth.of(2026, 5), 15, 5);
        ResumenCicloTarjeta actual = new ResumenCicloTarjeta(
                ciclo, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 5L, 25L, false, false, false
        );

        ResumenTarjeta resumen = new ResumenTarjeta(
                cuenta, actual, null, List.of(actual), List.of(), actual
        );

        assertEquals(cuenta, resumen.cuenta());
        assertEquals(actual, resumen.cicloActual());
        assertEquals(1, resumen.historialCiclos().size());
        assertEquals(actual, resumen.proximoPagoAlerta());
    }
}
