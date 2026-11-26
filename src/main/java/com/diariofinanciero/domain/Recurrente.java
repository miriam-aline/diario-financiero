package com.diariofinanciero.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "recurrente")
public class Recurrente {

    private static final int MAX_DESCRIPCION = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = MAX_DESCRIPCION)
    private String descripcion;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimiento tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Periodicidad periodicidad;

    @Column(name = "dia_referencia", nullable = false)
    private int diaReferencia;

    @Column(name = "proximo_cobro", nullable = false)
    private LocalDate proximoCobro;

    @Column(nullable = false)
    private boolean activo = true;

    protected Recurrente() {
    }

    public Recurrente(
            String descripcion,
            BigDecimal monto,
            TipoMovimiento tipo,
            Categoria categoria,
            Cuenta cuenta,
            Periodicidad periodicidad,
            LocalDate proximoCobro
    ) {
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripción del recurrente es obligatoria");
        }
        String limpia = descripcion.trim();
        if (limpia.length() > MAX_DESCRIPCION) {
            throw new IllegalArgumentException("La descripción no puede superar " + MAX_DESCRIPCION + " caracteres");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de movimiento es obligatorio");
        }
        if (categoria == null) {
            throw new IllegalArgumentException("La categoría es obligatoria");
        }
        if (cuenta == null) {
            throw new IllegalArgumentException("La cuenta es obligatoria");
        }
        if (periodicidad == null) {
            throw new IllegalArgumentException("La periodicidad es obligatoria");
        }
        if (proximoCobro == null) {
            throw new IllegalArgumentException("La fecha de próximo cobro es obligatoria");
        }

        this.descripcion = limpia;
        this.monto = Dinero.validarPositivo(monto);
        this.tipo = tipo;
        this.categoria = categoria;
        this.cuenta = cuenta;
        this.periodicidad = periodicidad;
        this.diaReferencia = proximoCobro.getDayOfMonth();
        this.proximoCobro = proximoCobro;
        this.activo = true;
    }

    public void avanzarProximoCobro() {
        this.proximoCobro = this.periodicidad.calcularSiguienteFecha(this.proximoCobro, this.diaReferencia);
    }

    public void alternarActivo() {
        this.activo = !this.activo;
    }

    public Long getId() {
        return id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public Cuenta getCuenta() {
        return cuenta;
    }

    public Periodicidad getPeriodicidad() {
        return periodicidad;
    }

    public int getDiaReferencia() {
        return diaReferencia;
    }

    public LocalDate getProximoCobro() {
        return proximoCobro;
    }

    public boolean isActivo() {
        return activo;
    }
}
