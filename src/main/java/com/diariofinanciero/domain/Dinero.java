package com.diariofinanciero.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Reglas monetarias del dominio: siempre BigDecimal, escala 2 y redondeo HALF_UP.
 */
public final class Dinero {

    public static final int ESCALA = 2;
    public static final RoundingMode REDONDEO = RoundingMode.HALF_UP;
    public static final BigDecimal CERO = BigDecimal.ZERO.setScale(ESCALA, REDONDEO);

    private Dinero() {
    }

    public static BigDecimal normalizar(BigDecimal monto) {
        if (monto == null) {
            throw new IllegalArgumentException("El monto es obligatorio");
        }
        return monto.setScale(ESCALA, REDONDEO);
    }

    public static BigDecimal validarPositivo(BigDecimal monto) {
        BigDecimal normalizado = normalizar(monto);
        if (normalizado.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero");
        }
        return normalizado;
    }

    public static BigDecimal sumar(BigDecimal a, BigDecimal b) {
        return normalizar(a).add(normalizar(b)).setScale(ESCALA, REDONDEO);
    }

    public static BigDecimal restar(BigDecimal a, BigDecimal b) {
        return normalizar(a).subtract(normalizar(b)).setScale(ESCALA, REDONDEO);
    }
}
