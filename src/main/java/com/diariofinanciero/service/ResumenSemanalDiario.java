package com.diariofinanciero.service;

import com.diariofinanciero.domain.EntradaDiario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ResumenSemanalDiario(
        LocalDate inicioSemana,
        LocalDate finSemana,
        BigDecimal totalGastado,
        BigDecimal totalIngresado,
        String categoriaMasAlta,
        BigDecimal montoCategoriaMasAlta,
        int diasSinGasto,
        int diasEvaluados,
        List<EntradaDiario> notasSemana,
        List<DiaDiario> dias
) {
}
