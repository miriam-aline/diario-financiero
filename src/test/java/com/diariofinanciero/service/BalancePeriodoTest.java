package com.diariofinanciero.service;

import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.Movimiento;
import com.diariofinanciero.domain.TipoCuenta;
import com.diariofinanciero.domain.TipoMovimiento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BalancePeriodoTest {

    @Test
    @DisplayName("Debe normalizar montos en constructor compacto y calcular balance desde movimientos")
    void normalizacionYCalculo() {
        BalancePeriodo directo = new BalancePeriodo(new BigDecimal("100"), new BigDecimal("40.5"), new BigDecimal("59.5"));
        assertEquals(new BigDecimal("100.00"), directo.totalIngresos());
        assertEquals(new BigDecimal("40.50"), directo.totalGastos());
        assertEquals(new BigDecimal("59.50"), directo.balance());

        BalancePeriodo vacio = BalancePeriodo.calcular(null);
        assertEquals(new BigDecimal("0.00"), vacio.balance());

        Categoria cat = new Categoria("General");
        Cuenta cta = new Cuenta("Efectivo", TipoCuenta.EFECTIVO);
        LocalDate fecha = LocalDate.of(2026, 5, 1);

        BalancePeriodo calculado = BalancePeriodo.calcular(List.of(
                Movimiento.manual(fecha, new BigDecimal("1000"), TipoMovimiento.INGRESO, cat, cta, "Salario"),
                Movimiento.manual(fecha, new BigDecimal("350.25"), TipoMovimiento.GASTO, cat, cta, "Super")
        ));
        assertEquals(new BigDecimal("1000.00"), calculado.totalIngresos());
        assertEquals(new BigDecimal("350.25"), calculado.totalGastos());
        assertEquals(new BigDecimal("649.75"), calculado.balance());
    }
}
