package com.diariofinanciero.service;

import com.diariofinanciero.domain.EntradaDiario;
import com.diariofinanciero.domain.Movimiento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public record DiaDiario(
        LocalDate fecha,
        String etiquetaDia,
        EntradaDiario entrada,
        List<Movimiento> movimientos,
        BigDecimal totalIngresos,
        BigDecimal totalGastos,
        BigDecimal balanceDia
) {

    private static final Locale LOCALE_MX = Locale.forLanguageTag("es-MX");

    public static String formatearDia(LocalDate fecha) {
        String nombreDia = fecha.getDayOfWeek().getDisplayName(TextStyle.FULL, LOCALE_MX);
        String diaCap = nombreDia.substring(0, 1).toUpperCase(LOCALE_MX) + nombreDia.substring(1);
        String nombreMes = fecha.getMonth().getDisplayName(TextStyle.FULL, LOCALE_MX);
        return diaCap + " " + fecha.getDayOfMonth() + " de " + nombreMes + " de " + fecha.getYear();
    }
}
