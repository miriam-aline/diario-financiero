package com.diariofinanciero.service;

import java.time.LocalDate;
import java.util.Locale;

public record FiltroMovimiento(
        LocalDate desde,
        LocalDate hasta,
        Long categoriaId,
        String texto
) {

    public FiltroMovimiento {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final");
        }
        texto = (texto == null || texto.isBlank()) ? null : texto.trim();
    }

    public static FiltroMovimiento vacio() {
        return new FiltroMovimiento(null, null, null, null);
    }

    public String patronBusquedaSql() {
        if (texto == null) {
            return null;
        }
        return "%" + texto.toLowerCase(Locale.ROOT) + "%";
    }
}
