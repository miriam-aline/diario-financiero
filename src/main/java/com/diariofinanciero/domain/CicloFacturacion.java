package com.diariofinanciero.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * Calcula ciclos de facturación y fechas límite de pago para tarjetas de crédito,
 * ajustando días 29, 30 y 31 al último día válido en meses cortos y años bisiestos.
 *
 * Regla bancaria estándar:
 * - Un ciclo abarca del día siguiente al corte anterior hasta el día de corte actual (inclusive).
 * - Si diaPago > diaCorte, la fecha límite de pago cae en el mismo mes del corte;
 *   si diaPago <= diaCorte, cae en el mes siguiente al corte.
 */
public record CicloFacturacion(
        LocalDate inicio,
        LocalDate fechaCorte,
        LocalDate fechaPago
) {

    public static CicloFacturacion cicloActual(int diaCorte, int diaPago, LocalDate fechaReferencia) {
        validarDia(diaCorte, "El día de corte");
        validarDia(diaPago, "El día de pago");
        LocalDate referencia = fechaReferencia == null ? LocalDate.now() : fechaReferencia;

        YearMonth mesReferencia = YearMonth.from(referencia);
        LocalDate corteMesReferencia = fechaEnMes(mesReferencia, diaCorte);

        YearMonth mesCorte = referencia.isAfter(corteMesReferencia)
                ? mesReferencia.plusMonths(1)
                : mesReferencia;

        return paraMesDeCorte(mesCorte, diaCorte, diaPago);
    }

    public static CicloFacturacion paraMesDeCorte(YearMonth mesCorte, int diaCorte, int diaPago) {
        validarDia(diaCorte, "El día de corte");
        validarDia(diaPago, "El día de pago");

        LocalDate corte = fechaEnMes(mesCorte, diaCorte);
        LocalDate corteAnterior = fechaEnMes(mesCorte.minusMonths(1), diaCorte);
        LocalDate inicio = corteAnterior.plusDays(1);

        YearMonth mesPago = diaPago > diaCorte ? mesCorte : mesCorte.plusMonths(1);
        LocalDate pago = fechaEnMes(mesPago, diaPago);

        return new CicloFacturacion(inicio, corte, pago);
    }

    public CicloFacturacion anterior(int diaCorte, int diaPago) {
        return paraMesDeCorte(YearMonth.from(fechaCorte).minusMonths(1), diaCorte, diaPago);
    }

    public long diasHastaCorte(LocalDate desde) {
        LocalDate base = desde == null ? LocalDate.now() : desde;
        return ChronoUnit.DAYS.between(base, fechaCorte);
    }

    public long diasHastaPago(LocalDate desde) {
        LocalDate base = desde == null ? LocalDate.now() : desde;
        return ChronoUnit.DAYS.between(base, fechaPago);
    }

    public static LocalDate fechaEnMes(YearMonth mes, int dia) {
        int diaAjustado = Math.min(dia, mes.lengthOfMonth());
        return mes.atDay(diaAjustado);
    }

    private static void validarDia(int dia, String campo) {
        if (dia < 1 || dia > 31) {
            throw new IllegalArgumentException(campo + " debe estar entre 1 y 31");
        }
    }
}
