package com.diariofinanciero.repository;

import java.math.BigDecimal;

public interface GastoPorCategoriaProjection {

    Long getCategoriaId();

    BigDecimal getTotal();
}
