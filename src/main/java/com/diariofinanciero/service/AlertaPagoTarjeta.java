package com.diariofinanciero.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AlertaPagoTarjeta(
        Long cuentaId,
        String nombreTarjeta,
        LocalDate fechaPago,
        long diasRestantes,
        BigDecimal estimadoAPagar,
        boolean esCicloCerrado
) {
}
