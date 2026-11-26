package com.diariofinanciero.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface GastoDiarioProjection {

    LocalDate getFecha();

    BigDecimal getTotal();
}
