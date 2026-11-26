package com.diariofinanciero.service;

import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.repository.CategoriaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public List<Categoria> listarTodas() {
        return categoriaRepository.findAllByOrderByNombreAsc();
    }

    public Categoria obtenerPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El identificador de categoría es obligatorio");
        }
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada con id: " + id));
    }

    public List<Categoria> listarRecientes(int limite) {
        if (limite <= 0) {
            throw new IllegalArgumentException("El límite de categorías recientes debe ser mayor que cero");
        }
        List<Categoria> recientes = categoriaRepository.findCategoriasRecientes(PageRequest.of(0, limite));
        if (!recientes.isEmpty()) {
            return recientes;
        }
        List<Categoria> todas = categoriaRepository.findAllByOrderByNombreAsc();
        return todas.subList(0, Math.min(limite, todas.size()));
    }

    @Transactional
    public Categoria crear(String nombre, BigDecimal presupuestoMensual) {
        Categoria nueva = new Categoria(nombre, presupuestoMensual);
        if (categoriaRepository.findByNombreIgnoreCase(nueva.getNombre()).isPresent()) {
            throw new IllegalArgumentException("Ya existe una categoría llamada \"" + nueva.getNombre() + "\"");
        }
        return categoriaRepository.save(nueva);
    }

    @Transactional
    public Categoria actualizarPresupuesto(Long categoriaId, BigDecimal presupuestoMensual) {
        Categoria categoria = obtenerPorId(categoriaId);
        categoria.actualizarPresupuesto(presupuestoMensual);
        return categoriaRepository.save(categoria);
    }
}
