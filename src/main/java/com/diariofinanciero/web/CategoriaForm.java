package com.diariofinanciero.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CategoriaForm {

    @NotBlank(message = "El nombre de la categoría es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String nombre;

    @DecimalMin(value = "0.01", message = "El presupuesto debe ser mayor que cero")
    @Digits(integer = 15, fraction = 2, message = "El presupuesto admite máximo 2 decimales")
    private BigDecimal presupuestoMensual;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getPresupuestoMensual() {
        return presupuestoMensual;
    }

    public void setPresupuestoMensual(BigDecimal presupuestoMensual) {
        this.presupuestoMensual = presupuestoMensual;
    }
}
