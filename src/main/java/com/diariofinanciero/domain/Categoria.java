package com.diariofinanciero.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "categoria")
public class Categoria {

    private static final int MAX_NOMBRE = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = MAX_NOMBRE)
    private String nombre;

    @Column(name = "presupuesto_mensual", precision = 19, scale = 2)
    private BigDecimal presupuestoMensual;

    protected Categoria() {
    }

    public Categoria(String nombre) {
        this(nombre, null);
    }

    public Categoria(String nombre, BigDecimal presupuestoMensual) {
        this.nombre = validarNombre(nombre);
        this.presupuestoMensual = presupuestoMensual == null ? null : Dinero.validarPositivo(presupuestoMensual);
    }

    public void actualizarPresupuesto(BigDecimal nuevoPresupuesto) {
        this.presupuestoMensual = nuevoPresupuesto == null ? null : Dinero.validarPositivo(nuevoPresupuesto);
    }

    private String validarNombre(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El nombre de la categoría es obligatorio");
        }
        String limpio = valor.trim();
        if (limpio.length() > MAX_NOMBRE) {
            throw new IllegalArgumentException("El nombre de la categoría no puede superar " + MAX_NOMBRE + " caracteres");
        }
        return limpio;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getPresupuestoMensual() {
        return presupuestoMensual;
    }
}
