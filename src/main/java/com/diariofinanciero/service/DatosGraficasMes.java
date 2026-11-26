package com.diariofinanciero.service;

import java.math.BigDecimal;
import java.util.List;

public record DatosGraficasMes(
        String mes,
        String etiquetaMes,
        BigDecimal totalIngresosMes,
        BigDecimal totalGastosMes,
        BigDecimal presupuestoTotalMes,
        List<ItemCategoriaGrafica> gastoPorCategoria,
        List<ItemMesGrafica> ingresosVsGastosMensual,
        List<ItemAcumuladoDia> gastoAcumuladoDiario
) {

    public record ItemCategoriaGrafica(
            String categoria,
            BigDecimal monto,
            BigDecimal porcentaje
    ) {
    }

    public record ItemMesGrafica(
            String claveMes,
            String etiqueta,
            BigDecimal ingresos,
            BigDecimal gastos
    ) {
    }

    public record ItemAcumuladoDia(
            int dia,
            String fecha,
            BigDecimal gastoDia,
            BigDecimal acumulado,
            BigDecimal presupuestoTotal
    ) {
    }
}
