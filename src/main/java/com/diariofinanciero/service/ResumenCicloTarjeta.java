package com.diariofinanciero.service;

import com.diariofinanciero.domain.CicloFacturacion;

import java.math.BigDecimal;

public record ResumenCicloTarjeta(
        CicloFacturacion ciclo,
        BigDecimal totalCargos,
        BigDecimal totalAbonos,
        BigDecimal estimadoAPagar,
        int cantidadMovimientos,
        long diasParaCorte,
        long diasParaPago,
        boolean pagoProximo,
        boolean pagoHoy,
        boolean cicloCerrado
) {
}
