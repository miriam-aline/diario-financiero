package com.diariofinanciero.service;

import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.Movimiento;

import java.util.List;

public record ResumenTarjeta(
        Cuenta cuenta,
        ResumenCicloTarjeta cicloActual,
        ResumenCicloTarjeta cicloAnterior,
        List<ResumenCicloTarjeta> historialCiclos,
        List<Movimiento> movimientosCicloActual,
        ResumenCicloTarjeta proximoPagoAlerta
) {
}
