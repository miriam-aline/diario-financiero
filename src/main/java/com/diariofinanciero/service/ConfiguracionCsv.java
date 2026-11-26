package com.diariofinanciero.service;

public record ConfiguracionCsv(
        Long cuentaId,
        String columnaFecha,
        String columnaDescripcion,
        String columnaCargo,
        String columnaAbono,
        String formatoFecha,
        char separador,
        boolean primeraFilaEsEncabezado
) {

    public ConfiguracionCsv {
        if (cuentaId == null) {
            throw new IllegalArgumentException("Selecciona la cuenta destino para la importación");
        }
        columnaFecha = limpiarRequerido(columnaFecha, "La columna de fecha es obligatoria");
        columnaDescripcion = limpiarRequerido(columnaDescripcion, "La columna de descripción es obligatoria");
        columnaCargo = limpiarOpcional(columnaCargo);
        columnaAbono = limpiarOpcional(columnaAbono);
        if (columnaCargo == null && columnaAbono == null) {
            throw new IllegalArgumentException("Indica al menos la columna de cargo o la de abono");
        }
        formatoFecha = limpiarRequerido(formatoFecha, "El formato de fecha es obligatorio");
    }

    private static String limpiarRequerido(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        return valor.trim();
    }

    private static String limpiarOpcional(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
