package com.diariofinanciero.service;

public record ResultadoConfirmacionImportacion(
        int importados,
        int omitidosDuplicados,
        int pendientesSinCategoria
) {
}
