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

@Entity
@Table(name = "regla_categoria")
public class ReglaCategoria {

    private static final int MAX_TEXTO = 120;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "texto_contiene", nullable = false, unique = true, length = MAX_TEXTO)
    private String textoContiene;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    protected ReglaCategoria() {
    }

    public ReglaCategoria(String textoContiene, Categoria categoria) {
        this.textoContiene = validarTexto(textoContiene);
        if (categoria == null) {
            throw new IllegalArgumentException("La categoría de la regla es obligatoria");
        }
        this.categoria = categoria;
    }

    public void actualizarCategoria(Categoria nuevaCategoria) {
        if (nuevaCategoria == null) {
            throw new IllegalArgumentException("La categoría de la regla es obligatoria");
        }
        this.categoria = nuevaCategoria;
    }

    public boolean coincide(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return false;
        }
        String descripcionNormalizada = HashImportacion.normalizarTexto(descripcion);
        return descripcionNormalizada.contains(this.textoContiene);
    }

    private String validarTexto(String valor) {
        String normalizado = HashImportacion.normalizarTexto(valor);
        if (normalizado.isEmpty()) {
            throw new IllegalArgumentException("El texto de la regla es obligatorio");
        }
        if (normalizado.length() > MAX_TEXTO) {
            throw new IllegalArgumentException("El texto de la regla no puede superar " + MAX_TEXTO + " caracteres");
        }
        return normalizado;
    }

    public Long getId() {
        return id;
    }

    public String getTextoContiene() {
        return textoContiene;
    }

    public Categoria getCategoria() {
        return categoria;
    }
}
