package com.diariofinanciero.domain;

public enum OrigenMovimiento {
    MANUAL("Manual"),
    IMPORTADO("Importado");

    private final String etiqueta;

    OrigenMovimiento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
