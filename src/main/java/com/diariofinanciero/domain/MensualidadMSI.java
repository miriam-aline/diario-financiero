package com.diariofinanciero.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MensualidadMSI(
        int numero,
        LocalDate fechaCargo,
        BigDecimal monto,
        boolean pagada
) {
}
