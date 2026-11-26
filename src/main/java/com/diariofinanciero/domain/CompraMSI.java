package com.diariofinanciero.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "compra_msi")
public class CompraMSI {

    private static final int MAX_DESCRIPCION = 255;
    private static final BigDecimal UN_CENTAVO = new BigDecimal("0.01");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = MAX_DESCRIPCION)
    private String descripcion;

    @Column(name = "monto_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "numero_mensualidades", nullable = false)
    private int numeroMensualidades;

    @Column(name = "mensualidades_pagadas", nullable = false)
    private int mensualidadesPagadas;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(name = "fecha_primer_cargo", nullable = false)
    private LocalDate fechaPrimerCargo;

    protected CompraMSI() {
    }

    public CompraMSI(
            String descripcion,
            BigDecimal montoTotal,
            int numeroMensualidades,
            int mensualidadesPagadas,
            Cuenta cuenta,
            Categoria categoria,
            LocalDate fechaPrimerCargo
    ) {
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripción de la compra a MSI es obligatoria");
        }
        String limpia = descripcion.trim();
        if (limpia.length() > MAX_DESCRIPCION) {
            throw new IllegalArgumentException("La descripción no puede superar " + MAX_DESCRIPCION + " caracteres");
        }
        if (numeroMensualidades < 2) {
            throw new IllegalArgumentException("El número de mensualidades debe ser al menos 2");
        }
        if (mensualidadesPagadas < 0 || mensualidadesPagadas > numeroMensualidades) {
            throw new IllegalArgumentException("Las mensualidades pagadas deben estar entre 0 y " + numeroMensualidades);
        }
        if (cuenta == null) {
            throw new IllegalArgumentException("La cuenta es obligatoria");
        }
        if (fechaPrimerCargo == null) {
            throw new IllegalArgumentException("La fecha del primer cargo es obligatoria");
        }

        this.descripcion = limpia;
        this.montoTotal = Dinero.validarPositivo(montoTotal);
        this.numeroMensualidades = numeroMensualidades;
        this.mensualidadesPagadas = mensualidadesPagadas;
        this.cuenta = cuenta;
        this.categoria = categoria;
        this.fechaPrimerCargo = fechaPrimerCargo;
    }

    /**
     * Genera el plan completo de mensualidades repartiendo los centavos sobrantes
     * (+$0.01 en las primeras k cuotas) para garantizar que la suma coincida exactamente con montoTotal.
     */
    public List<MensualidadMSI> calcularPlanMensualidades() {
        BigDecimal divisor = BigDecimal.valueOf(numeroMensualidades);
        BigDecimal cuotaBase = montoTotal.divide(divisor, Dinero.ESCALA, RoundingMode.DOWN);
        BigDecimal totalBase = cuotaBase.multiply(divisor);
        int centavosSobrantes = montoTotal.subtract(totalBase)
                .movePointRight(Dinero.ESCALA)
                .intValueExact();

        int diaReferencia = fechaPrimerCargo.getDayOfMonth();
        YearMonth mesInicial = YearMonth.from(fechaPrimerCargo);
        List<MensualidadMSI> plan = new ArrayList<>(numeroMensualidades);

        for (int i = 0; i < numeroMensualidades; i++) {
            BigDecimal montoCuota = i < centavosSobrantes
                    ? Dinero.sumar(cuotaBase, UN_CENTAVO)
                    : cuotaBase;
            YearMonth mesCargo = mesInicial.plusMonths(i);
            LocalDate fechaCargo = mesCargo.atDay(Math.min(diaReferencia, mesCargo.lengthOfMonth()));
            boolean pagada = (i + 1) <= mensualidadesPagadas;
            plan.add(new MensualidadMSI(i + 1, fechaCargo, montoCuota, pagada));
        }

        return List.copyOf(plan);
    }

    public int mensualidadesRestantes() {
        return numeroMensualidades - mensualidadesPagadas;
    }

    public boolean estaLiquidada() {
        return mensualidadesPagadas >= numeroMensualidades;
    }

    public int porcentajeAvance() {
        return Math.round((mensualidadesPagadas * 100.0f) / numeroMensualidades);
    }

    public BigDecimal montoPagado() {
        BigDecimal suma = Dinero.CERO;
        for (MensualidadMSI m : calcularPlanMensualidades()) {
            if (m.pagada()) {
                suma = Dinero.sumar(suma, m.monto());
            }
        }
        return suma;
    }

    public BigDecimal saldoPendiente() {
        return Dinero.restar(montoTotal, montoPagado());
    }

    public MensualidadMSI proximaMensualidad() {
        if (estaLiquidada()) {
            return null;
        }
        return calcularPlanMensualidades().get(mensualidadesPagadas);
    }

    public MensualidadMSI registrarPagoSiguienteMensualidad() {
        if (estaLiquidada()) {
            throw new IllegalStateException("Esta compra a MSI ya está completamente pagada");
        }
        MensualidadMSI cuota = calcularPlanMensualidades().get(mensualidadesPagadas);
        this.mensualidadesPagadas++;
        return cuota;
    }

    public Long getId() {
        return id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public int getNumeroMensualidades() {
        return numeroMensualidades;
    }

    public int getMensualidadesPagadas() {
        return mensualidadesPagadas;
    }

    public Cuenta getCuenta() {
        return cuenta;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public LocalDate getFechaPrimerCargo() {
        return fechaPrimerCargo;
    }
}
