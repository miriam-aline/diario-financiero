package com.diariofinanciero.domain;

public enum TipoCuenta {
    EFECTIVO("Efectivo"),
    DEBITO("Débito"),
    CREDITO("Crédito");

    private final String etiqueta;

    TipoCuenta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
