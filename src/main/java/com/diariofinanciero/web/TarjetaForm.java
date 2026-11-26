package com.diariofinanciero.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TarjetaForm {

    @NotBlank(message = "El nombre de la tarjeta es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String nombre;

    @NotNull(message = "El día de corte es obligatorio")
    @Min(value = 1, message = "El día de corte debe estar entre 1 y 31")
    @Max(value = 31, message = "El día de corte debe estar entre 1 y 31")
    private Integer diaCorte;

    @NotNull(message = "El día de pago es obligatorio")
    @Min(value = 1, message = "El día de pago debe estar entre 1 y 31")
    @Max(value = 31, message = "El día de pago debe estar entre 1 y 31")
    private Integer diaPago;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getDiaCorte() {
        return diaCorte;
    }

    public void setDiaCorte(Integer diaCorte) {
        this.diaCorte = diaCorte;
    }

    public Integer getDiaPago() {
        return diaPago;
    }

    public void setDiaPago(Integer diaPago) {
        this.diaPago = diaPago;
    }
}
