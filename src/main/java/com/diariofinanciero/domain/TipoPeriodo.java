package com.diariofinanciero.domain;

public enum TipoPeriodo {
    MES("Mes completo"),
    PRIMERA_QUINCENA("1ª quincena (1–15)"),
    SEGUNDA_QUINCENA("2ª quincena (16–fin)");

    private final String etiqueta;

    TipoPeriodo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
