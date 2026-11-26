package com.diariofinanciero.domain;

import java.math.BigDecimal;

public enum EstadoPresupuesto {
    SIN_PRESUPUESTO("Sin presupuesto", 3),
    NORMAL("Normal", 2),
    AVISO("Aviso", 1),
    EXCEDIDO("Excedido", 0);

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal UMBRAL_AVISO = BigDecimal.valueOf(80);

    private final String etiqueta;
    private final int prioridadOrden;

    EstadoPresupuesto(String etiqueta, int prioridadOrden) {
        this.etiqueta = etiqueta;
        this.prioridadOrden = prioridadOrden;
    }

    /**
     * Evalúa el estado sin errores de redondeo:
     * - Menos de 80 % -> NORMAL
     * - 80 % a 99.99... % -> AVISO
     * - 100 % o más -> EXCEDIDO
     */
    public static EstadoPresupuesto evaluar(BigDecimal gastado, BigDecimal presupuesto) {
        if (presupuesto == null) {
            return SIN_PRESUPUESTO;
        }
        BigDecimal gastadoNormalizado = gastado == null ? Dinero.CERO : Dinero.normalizar(gastado);
        BigDecimal presupuestoNormalizado = Dinero.validarPositivo(presupuesto);

        if (gastadoNormalizado.compareTo(presupuestoNormalizado) >= 0) {
            return EXCEDIDO;
        }
        if (gastadoNormalizado.multiply(CIEN).compareTo(presupuestoNormalizado.multiply(UMBRAL_AVISO)) >= 0) {
            return AVISO;
        }
        return NORMAL;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public int getPrioridadOrden() {
        return prioridadOrden;
    }
}
