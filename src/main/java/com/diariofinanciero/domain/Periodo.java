package com.diariofinanciero.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Representa un periodo contable (mes completo o quincena) resolviendo correctamente
 * meses de 28, 29 (bisiestos), 30 y 31 días.
 */
public record Periodo(
        YearMonth mes,
        TipoPeriodo tipo,
        LocalDate desde,
        LocalDate hasta
) {

    private static final Locale LOCALE_MX = Locale.forLanguageTag("es-MX");

    public static Periodo de(YearMonth mes, TipoPeriodo tipo) {
        if (mes == null) {
            throw new IllegalArgumentException("El mes del periodo es obligatorio");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de periodo es obligatorio");
        }

        return switch (tipo) {
            case MES -> new Periodo(mes, tipo, mes.atDay(1), mes.atEndOfMonth());
            case PRIMERA_QUINCENA -> new Periodo(mes, tipo, mes.atDay(1), mes.atDay(15));
            case SEGUNDA_QUINCENA -> new Periodo(mes, tipo, mes.atDay(16), mes.atEndOfMonth());
        };
    }

    public static Periodo actual(TipoPeriodo tipo) {
        return de(YearMonth.now(), tipo);
    }

    public Periodo anterior() {
        return switch (tipo) {
            case MES -> de(mes.minusMonths(1), TipoPeriodo.MES);
            case PRIMERA_QUINCENA -> de(mes.minusMonths(1), TipoPeriodo.SEGUNDA_QUINCENA);
            case SEGUNDA_QUINCENA -> de(mes, TipoPeriodo.PRIMERA_QUINCENA);
        };
    }

    public Periodo siguiente() {
        return switch (tipo) {
            case MES -> de(mes.plusMonths(1), TipoPeriodo.MES);
            case PRIMERA_QUINCENA -> de(mes, TipoPeriodo.SEGUNDA_QUINCENA);
            case SEGUNDA_QUINCENA -> de(mes.plusMonths(1), TipoPeriodo.PRIMERA_QUINCENA);
        };
    }

    public String etiqueta() {
        String nombreMes = mes.getMonth().getDisplayName(TextStyle.FULL, LOCALE_MX);
        String mesCapitalizado = nombreMes.substring(0, 1).toUpperCase(LOCALE_MX) + nombreMes.substring(1);
        return switch (tipo) {
            case MES -> mesCapitalizado + " " + mes.getYear();
            case PRIMERA_QUINCENA -> mesCapitalizado + " " + mes.getYear() + " · 1ª quincena (1–15)";
            case SEGUNDA_QUINCENA -> mesCapitalizado + " " + mes.getYear() + " · 2ª quincena (16–" + hasta.getDayOfMonth() + ")";
        };
    }
}
