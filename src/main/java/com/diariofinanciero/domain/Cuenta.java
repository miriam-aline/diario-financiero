package com.diariofinanciero.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cuenta")
public class Cuenta {

    private static final int MAX_NOMBRE = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = MAX_NOMBRE)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCuenta tipo;

    @Column(name = "dia_corte")
    private Integer diaCorte;

    @Column(name = "dia_pago")
    private Integer diaPago;

    protected Cuenta() {
    }

    public Cuenta(String nombre, TipoCuenta tipo) {
        this(nombre, tipo, null, null);
    }

    public Cuenta(String nombre, TipoCuenta tipo, Integer diaCorte, Integer diaPago) {
        this.nombre = validarNombre(nombre);
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de cuenta es obligatorio");
        }
        this.tipo = tipo;
        if (tipo != TipoCuenta.CREDITO && (diaCorte != null || diaPago != null)) {
            throw new IllegalArgumentException("Solo las cuentas de crédito pueden tener día de corte o de pago");
        }
        this.diaCorte = validarDiaMes(diaCorte, "El día de corte");
        this.diaPago = validarDiaMes(diaPago, "El día de pago");
    }

    public void actualizarTarjetaCredito(String nuevoNombre, Integer nuevoDiaCorte, Integer nuevoDiaPago) {
        if (this.tipo != TipoCuenta.CREDITO) {
            throw new IllegalArgumentException("Solo las cuentas de crédito pueden tener día de corte y pago");
        }
        if (nuevoDiaCorte == null || nuevoDiaPago == null) {
            throw new IllegalArgumentException("El día de corte y el día de pago son obligatorios para una tarjeta de crédito");
        }
        this.nombre = validarNombre(nuevoNombre);
        this.diaCorte = validarDiaMes(nuevoDiaCorte, "El día de corte");
        this.diaPago = validarDiaMes(nuevoDiaPago, "El día de pago");
    }

    private String validarNombre(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El nombre de la cuenta es obligatorio");
        }
        String limpio = valor.trim();
        if (limpio.length() > MAX_NOMBRE) {
            throw new IllegalArgumentException("El nombre de la cuenta no puede superar " + MAX_NOMBRE + " caracteres");
        }
        return limpio;
    }

    private Integer validarDiaMes(Integer dia, String campo) {
        if (dia == null) {
            return null;
        }
        if (dia < 1 || dia > 31) {
            throw new IllegalArgumentException(campo + " debe estar entre 1 y 31");
        }
        return dia;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoCuenta getTipo() {
        return tipo;
    }

    public Integer getDiaCorte() {
        return diaCorte;
    }

    public Integer getDiaPago() {
        return diaPago;
    }
}
