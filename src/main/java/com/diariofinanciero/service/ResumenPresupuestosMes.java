package com.diariofinanciero.service;

import com.diariofinanciero.domain.EstadoPresupuesto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record ResumenPresupuestosMes(
        YearMonth mes,
        YearMonth mesAnterior,
        String etiquetaMes,
        String etiquetaMesAnterior,
        BigDecimal presupuestoTotal,
        BigDecimal gastadoTotalMes,
        BigDecimal gastadoTotalMesAnterior,
        BigDecimal diferenciaTotal,
        BigDecimal porcentajeAvanceGlobal,
        int porcentajeBarraGlobal,
        EstadoPresupuesto estadoGlobal,
        List<ResumenCategoriaPresupuesto> categorias
) {
}
