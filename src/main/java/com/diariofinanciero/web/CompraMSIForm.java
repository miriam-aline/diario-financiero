package com.diariofinanciero.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CompraMSIForm {

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
    private String descripcion;

    @NotNull(message = "El monto total es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto total debe ser mayor que cero")
    @Digits(integer = 15, fraction = 2, message = "El monto admite máximo 2 decimales")
    private BigDecimal montoTotal;

    @NotNull(message = "El número de mensualidades es obligatorio")
    @Min(value = 2, message = "Debe ser al menos 2 mensualidades")
    private Integer numeroMensualidades = 6;

    @NotNull(message = "Indica cuántas mensualidades van pagadas")
    @Min(value = 0, message = "No puede ser negativo")
    private Integer mensualidadesPagadas = 0;

    @NotNull(message = "Selecciona una cuenta")
    private Long cuentaId;

    private Long categoriaId;

    @NotNull(message = "La fecha del primer cargo es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaPrimerCargo = LocalDate.now();

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }

    public Integer getNumeroMensualidades() {
        return numeroMensualidades;
    }

    public void setNumeroMensualidades(Integer numeroMensualidades) {
        this.numeroMensualidades = numeroMensualidades;
    }

    public Integer getMensualidadesPagadas() {
        return mensualidadesPagadas;
    }

    public void setMensualidadesPagadas(Integer mensualidadesPagadas) {
        this.mensualidadesPagadas = mensualidadesPagadas;
    }

    public Long getCuentaId() {
        return cuentaId;
    }

    public void setCuentaId(Long cuentaId) {
        this.cuentaId = cuentaId;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public LocalDate getFechaPrimerCargo() {
        return fechaPrimerCargo;
    }

    public void setFechaPrimerCargo(LocalDate fechaPrimerCargo) {
        this.fechaPrimerCargo = fechaPrimerCargo;
    }
}
