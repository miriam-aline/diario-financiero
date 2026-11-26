package com.diariofinanciero.web;

import com.diariofinanciero.domain.EntradaDiario;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class EntradaDiarioForm {

    @NotNull(message = "La fecha es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha = LocalDate.now();

    @NotBlank(message = "Escribe una nota para guardar la entrada")
    @Size(max = 4000, message = "La nota no puede superar 4000 caracteres")
    private String nota;

    @Min(value = 1, message = "El estado de ánimo debe estar entre 1 y 5")
    @Max(value = 5, message = "El estado de ánimo debe estar entre 1 y 5")
    private Integer estadoDeAnimo;

    @Size(max = 500, message = "Las etiquetas no pueden superar 500 caracteres")
    private String etiquetas;

    public static EntradaDiarioForm desdeEntidad(EntradaDiario entrada) {
        EntradaDiarioForm form = new EntradaDiarioForm();
        form.setFecha(entrada.getFecha());
        form.setNota(entrada.getNota());
        form.setEstadoDeAnimo(entrada.getEstadoDeAnimo());
        form.setEtiquetas(entrada.getEtiquetas() == null ? "" : entrada.getEtiquetas().replace(",", ", "));
        return form;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }

    public Integer getEstadoDeAnimo() {
        return estadoDeAnimo;
    }

    public void setEstadoDeAnimo(Integer estadoDeAnimo) {
        this.estadoDeAnimo = estadoDeAnimo;
    }

    public String getEtiquetas() {
        return etiquetas;
    }

    public void setEtiquetas(String etiquetas) {
        this.etiquetas = etiquetas;
    }
}
