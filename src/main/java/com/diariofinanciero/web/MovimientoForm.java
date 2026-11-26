package com.diariofinanciero.web;

import com.diariofinanciero.domain.Movimiento;
import com.diariofinanciero.domain.TipoMovimiento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MovimientoForm {

    @NotNull(message = "La fecha es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha = LocalDate.now();

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero")
    @Digits(integer = 15, fraction = 2, message = "El monto admite máximo 2 decimales")
    private BigDecimal monto;

    @NotNull(message = "El tipo de movimiento es obligatorio")
    private TipoMovimiento tipo = TipoMovimiento.GASTO;

    @NotNull(message = "Selecciona una categoría")
    private Long categoriaId;

    @NotNull(message = "Selecciona una cuenta")
    private Long cuentaId;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
    private String descripcion;

    public static MovimientoForm desdeEntidad(Movimiento movimiento) {
        MovimientoForm form = new MovimientoForm();
        form.setFecha(movimiento.getFecha());
        form.setMonto(movimiento.getMonto());
        form.setTipo(movimiento.getTipo());
        form.setCategoriaId(movimiento.getCategoria().getId());
        form.setCuentaId(movimiento.getCuenta().getId());
        form.setDescripcion(movimiento.getDescripcion());
        return form;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimiento tipo) {
        this.tipo = tipo;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public Long getCuentaId() {
        return cuentaId;
    }

    public void setCuentaId(Long cuentaId) {
        this.cuentaId = cuentaId;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
