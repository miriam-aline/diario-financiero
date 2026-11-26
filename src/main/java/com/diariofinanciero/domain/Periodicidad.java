package com.diariofinanciero.domain;

import java.time.LocalDate;
import java.time.YearMonth;

public enum Periodicidad {
    QUINCENAL("Quincenal"),
    MENSUAL("Mensual"),
    ANUAL("Anual");

    private final String etiqueta;

    Periodicidad(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /**
     * Calcula la siguiente fecha respetando el día de referencia original (29, 30 o 31)
     * en meses cortos y años bisiestos sin perderlo en meses posteriores.
     */
    public LocalDate calcularSiguienteFecha(LocalDate fechaActual, int diaReferencia) {
        if (fechaActual == null) {
            throw new IllegalArgumentException("La fecha actual es obligatoria");
        }
        int dia = Math.max(1, Math.min(31, diaReferencia));

        return switch (this) {
            case MENSUAL -> {
                YearMonth siguienteMes = YearMonth.from(fechaActual).plusMonths(1);
                yield siguienteMes.atDay(Math.min(dia, siguienteMes.lengthOfMonth()));
            }
            case ANUAL -> {
                YearMonth siguienteAnio = YearMonth.from(fechaActual).plusYears(1);
                yield siguienteAnio.atDay(Math.min(dia, siguienteAnio.lengthOfMonth()));
            }
            case QUINCENAL -> {
                // Quincena estándar (día 15 y fin de mes) cuando la referencia es 15 o fin de mes (>= 28)
                if (dia == 15 || dia >= 28) {
                    YearMonth mesActual = YearMonth.from(fechaActual);
                    if (fechaActual.getDayOfMonth() <= 15) {
                        yield mesActual.atEndOfMonth();
                    }
                    yield mesActual.plusMonths(1).atDay(15);
                }
                yield fechaActual.plusDays(15);
            }
        };
    }
}
