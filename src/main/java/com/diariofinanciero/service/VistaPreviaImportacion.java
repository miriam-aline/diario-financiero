package com.diariofinanciero.service;

import java.time.Instant;
import java.util.List;

public record VistaPreviaImportacion(
        String tokenConfirmacion,
        Long cuentaId,
        String nombreCuenta,
        Instant creadaEn,
        int totalFilasLeidas,
        int totalNuevos,
        int totalDuplicados,
        int totalOmitidas,
        int totalSinCategoria,
        List<FilaImportacionPreview> filas,
        List<String> avisosOmitidas
) {
}
