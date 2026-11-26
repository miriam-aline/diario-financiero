package com.diariofinanciero.service;

import com.diariofinanciero.domain.Dinero;
import com.diariofinanciero.domain.Movimiento;
import com.diariofinanciero.domain.TipoMovimiento;

import java.math.BigDecimal;
import java.util.List;

public record BalancePeriodo(
        BigDecimal totalIngresos,
        BigDecimal totalGastos,
        BigDecimal balance
) {

    public BalancePeriodo {
        totalIngresos = Dinero.normalizar(totalIngresos);
        totalGastos = Dinero.normalizar(totalGastos);
        balance = Dinero.normalizar(balance);
    }

    public static BalancePeriodo calcular(List<Movimiento> movimientos) {
        BigDecimal ingresos = Dinero.CERO;
        BigDecimal gastos = Dinero.CERO;

        if (movimientos != null) {
            for (Movimiento movimiento : movimientos) {
                if (movimiento.getTipo() == TipoMovimiento.INGRESO) {
                    ingresos = Dinero.sumar(ingresos, movimiento.getMonto());
                } else if (movimiento.getTipo() == TipoMovimiento.GASTO) {
                    gastos = Dinero.sumar(gastos, movimiento.getMonto());
                }
            }
        }

        return new BalancePeriodo(ingresos, gastos, Dinero.restar(ingresos, gastos));
    }
}
