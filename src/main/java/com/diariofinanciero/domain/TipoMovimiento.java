package com.diariofinanciero.domain;

public enum TipoMovimiento {
    INGRESO("Ingreso"),
    GASTO("Gasto");

    private final String etiqueta;

    TipoMovimiento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
