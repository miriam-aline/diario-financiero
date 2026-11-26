package com.diariofinanciero.service;

import com.diariofinanciero.domain.TipoMovimiento;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FilaImportacionPreview(
        int numeroLinea,
        LocalDate fecha,
        String descripcion,
        TipoMovimiento tipo,
        BigDecimal monto,
        Long categoriaSugeridaId,
        String categoriaSugeridaNombre,
        String reglaAplicada,
        String hashImportacion,
        boolean duplicado,
        boolean sinCategoria
) {
}
