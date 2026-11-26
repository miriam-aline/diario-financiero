package com.diariofinanciero.service;

import com.diariofinanciero.domain.EstadoPresupuesto;

import java.math.BigDecimal;

public record ResumenCategoriaPresupuesto(
        Long categoriaId,
        String categoriaNombre,
        BigDecimal presupuestoMensual,
        BigDecimal gastadoMes,
        BigDecimal gastadoMesAnterior,
        BigDecimal diferenciaMesAnterior,
        BigDecimal porcentajeCambioMesAnterior,
        BigDecimal porcentajeAvance,
        int porcentajeBarra,
        BigDecimal disponible,
        EstadoPresupuesto estado
) {
}
