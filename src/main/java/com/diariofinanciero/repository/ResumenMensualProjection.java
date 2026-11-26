package com.diariofinanciero.repository;

import com.diariofinanciero.domain.TipoMovimiento;

import java.math.BigDecimal;

public interface ResumenMensualProjection {

    int getAnio();

    int getMes();

    TipoMovimiento getTipo();

    BigDecimal getTotal();
}
