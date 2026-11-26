package com.diariofinanciero.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Entity
@Table(name = "entrada_diario")
public class EntradaDiario {

    private static final int MAX_NOTA = 4000;
    private static final int MAX_ETIQUETAS = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate fecha;

    @Column(nullable = false, length = MAX_NOTA)
    private String nota;

    @Column(name = "estado_de_animo")
    private Integer estadoDeAnimo;

    @Column(length = MAX_ETIQUETAS)
    private String etiquetas;

    protected EntradaDiario() {
    }

    public EntradaDiario(LocalDate fecha, String nota, Integer estadoDeAnimo, String etiquetas) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de la entrada de diario es obligatoria");
        }
        this.fecha = fecha;
        asignarContenido(nota, estadoDeAnimo, etiquetas);
    }

    public void actualizar(String nota, Integer estadoDeAnimo, String etiquetas) {
        asignarContenido(nota, estadoDeAnimo, etiquetas);
    }

    private void asignarContenido(String nota, Integer estadoDeAnimo, String etiquetas) {
        if (nota == null || nota.isBlank()) {
            throw new IllegalArgumentException("La nota del diario es obligatoria");
        }
        String notaLimpia = nota.trim();
        if (notaLimpia.length() > MAX_NOTA) {
            throw new IllegalArgumentException("La nota no puede superar " + MAX_NOTA + " caracteres");
        }
        if (estadoDeAnimo != null && (estadoDeAnimo < 1 || estadoDeAnimo > 5)) {
            throw new IllegalArgumentException("El estado de ánimo debe estar entre 1 y 5");
        }

        String etiquetasNormalizadas = normalizarEtiquetas(etiquetas);
        if (etiquetasNormalizadas != null && etiquetasNormalizadas.length() > MAX_ETIQUETAS) {
            throw new IllegalArgumentException("Las etiquetas no pueden superar " + MAX_ETIQUETAS + " caracteres");
        }

        this.nota = notaLimpia;
        this.estadoDeAnimo = estadoDeAnimo;
        this.etiquetas = etiquetasNormalizadas;
    }

    public static String normalizarEtiquetas(String etiquetasRaw) {
        if (etiquetasRaw == null || etiquetasRaw.isBlank()) {
            return null;
        }
        Set<String> limpias = new LinkedHashSet<>();
        for (String parte : etiquetasRaw.split(",")) {
            String token = parte.trim();
            while (token.startsWith("#")) {
                token = token.substring(1).trim();
            }
            if (!token.isEmpty()) {
                limpias.add(token.toLowerCase(Locale.ROOT));
            }
        }
        return limpias.isEmpty() ? null : String.join(",", limpias);
    }

    public List<String> listaEtiquetas() {
        if (etiquetas == null || etiquetas.isBlank()) {
            return List.of();
        }
        return Arrays.stream(etiquetas.split(","))
                .filter(s -> !s.isBlank())
                .toList();
    }

    public String etiquetaAnimo() {
        if (estadoDeAnimo == null) {
            return "";
        }
        return switch (estadoDeAnimo) {
            case 1 -> "1/5 · Muy estresado";
            case 2 -> "2/5 · Inquieto";
            case 3 -> "3/5 · Neutral";
            case 4 -> "4/5 · Tranquilo";
            case 5 -> "5/5 · Muy satisfecho";
            default -> "";
        };
    }

    public Long getId() {
        return id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getNota() {
        return nota;
    }

    public Integer getEstadoDeAnimo() {
        return estadoDeAnimo;
    }

    public String getEtiquetas() {
        return etiquetas;
    }
}
