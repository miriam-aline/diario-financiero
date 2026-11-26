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
@Table(name = "movimiento")
public class Movimiento {

    private static final int MAX_DESCRIPCION = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

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

    @Column(nullable = false, length = MAX_DESCRIPCION)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigenMovimiento origen;

    @Column(name = "hash_importacion", length = 64)
    private String hashImportacion;

    protected Movimiento() {
    }

    public Movimiento(
            LocalDate fecha,
            BigDecimal monto,
            TipoMovimiento tipo,
            Categoria categoria,
            Cuenta cuenta,
            String descripcion,
            OrigenMovimiento origen,
            String hashImportacion
    ) {
        if (origen == null) {
            throw new IllegalArgumentException("El origen del movimiento es obligatorio");
        }
        asignarDatos(fecha, monto, tipo, categoria, cuenta, descripcion);
        this.origen = origen;
        this.hashImportacion = (hashImportacion == null || hashImportacion.isBlank())
                ? null
                : hashImportacion.trim();
    }

    public static Movimiento manual(
            LocalDate fecha,
            BigDecimal monto,
            TipoMovimiento tipo,
            Categoria categoria,
            Cuenta cuenta,
            String descripcion
    ) {
        return new Movimiento(fecha, monto, tipo, categoria, cuenta, descripcion, OrigenMovimiento.MANUAL, null);
    }

    public void actualizar(
            LocalDate fecha,
            BigDecimal monto,
            TipoMovimiento tipo,
            Categoria categoria,
            Cuenta cuenta,
            String descripcion
    ) {
        asignarDatos(fecha, monto, tipo, categoria, cuenta, descripcion);
    }

    private void asignarDatos(
            LocalDate fecha,
            BigDecimal monto,
            TipoMovimiento tipo,
            Categoria categoria,
            Cuenta cuenta,
            String descripcion
    ) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
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
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripción es obligatoria");
        }
        String descripcionLimpia = descripcion.trim();
        if (descripcionLimpia.length() > MAX_DESCRIPCION) {
            throw new IllegalArgumentException("La descripción no puede superar " + MAX_DESCRIPCION + " caracteres");
        }

        this.fecha = fecha;
        this.monto = Dinero.validarPositivo(monto);
        this.tipo = tipo;
        this.categoria = categoria;
        this.cuenta = cuenta;
        this.descripcion = descripcionLimpia;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getFecha() {
        return fecha;
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

    public String getDescripcion() {
        return descripcion;
    }

    public OrigenMovimiento getOrigen() {
        return origen;
    }

    public String getHashImportacion() {
        return hashImportacion;
    }
}
